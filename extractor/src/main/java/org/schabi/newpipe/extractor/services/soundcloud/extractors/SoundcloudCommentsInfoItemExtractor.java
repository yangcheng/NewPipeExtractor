package org.schabi.newpipe.extractor.services.soundcloud.extractors;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;

import org.schabi.newpipe.extractor.ListExtractor;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.Page;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.comments.CommentsInfoItem;
import org.schabi.newpipe.extractor.comments.CommentsInfoItemExtractor;
import org.schabi.newpipe.extractor.comments.CommentsInfoItemsCollector;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.localization.DateWrapper;
import org.schabi.newpipe.extractor.services.soundcloud.SoundcloudParsingHelper;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class SoundcloudCommentsInfoItemExtractor implements CommentsInfoItemExtractor {
    private final JsonArray allItems;
    private final int index;
    private final JsonObject item;
    private final String url;

    public SoundcloudCommentsInfoItemExtractor(final JsonArray allItems, final int index, final JsonObject item, final String url) {
        this.allItems = allItems;
        this.index = index;
        this.item = item;
        this.url = url;
    }

    @Override
    public String getCommentId() {
        return Objects.toString(item.getLong("id"), null);
    }

    @Override
    public String getCommentText() {
        return item.getString("body");
    }

    @Override
    public String getUploaderName() {
        return item.getObject("user").getString("username");
    }

    @Override
    public String getUploaderAvatarUrl() {
        return item.getObject("user").getString("avatar_url");
    }

    @Override
    public boolean isUploaderVerified() throws ParsingException {
        return item.getObject("user").getBoolean("verified");
    }

    @Override
    public int getStreamPosition() throws ParsingException {
        return item.getInt("timestamp") / 1000; // convert milliseconds to seconds
    }

    @Override
    public String getUploaderUrl() {
        return item.getObject("user").getString("permalink_url");
    }

    @Override
    public String getTextualUploadDate() {
        return item.getString("created_at");
    }

    @Nullable
    @Override
    public DateWrapper getUploadDate() throws ParsingException {
        return new DateWrapper(SoundcloudParsingHelper.parseDateFrom(getTextualUploadDate()));
    }

    @Override
    public String getName() throws ParsingException {
        return item.getObject("user").getString("permalink");
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public String getThumbnailUrl() {
        return item.getObject("user").getString("avatar_url");
    }

    @Override
    public Page getReplies() {
        final CommentsInfoItemsCollector collector = new CommentsInfoItemsCollector(ServiceList.SoundCloud.getServiceId());

        // Replies start with the mention of the user who created the comment that is replies to.
        final String mention = "@" + item.getObject("user").getString("permalink");
        // Loop through all comments which come after this comment to find the replies to this comment.
        for (int i = index + 1; i < allItems.size(); i++) {
            final JsonObject comment = allItems.getObject(i);
            final String commentContent = comment.getString("body");
            if (commentContent.startsWith(mention)) {
                collector.commit(new SoundcloudCommentsInfoItemExtractor(allItems, i, comment, url));
            } else if (!commentContent.startsWith("@") || collector.getItems().isEmpty()) {
                // Only the comments directly after the original comment
                // starting with the mention of the comment's creator are replies to that comment.
                // The first comment not starting with these letters is the next top-level comment.
                break;
            }
        }
        if (collector.getItems().isEmpty()) {
            return null;
        }
        return new ListExtractor.InfoItemsPage<CommentsInfoItem>(collector, null);
        //return new ListExtractor.InfoItemsPage<CommentsInfoItem>(replies, null, Collections.emptyList());
    }
    
    @Override
    public int getReplyCount() throws ParsingException {
        return item.getInt("");
    }
}
