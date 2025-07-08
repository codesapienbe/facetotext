package net.codesapien;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.server.StreamResource;

/**
 * A Vaadin component for displaying HTML5 video, similar to Vaadin's Image component.
 */
@Tag("video")
public class Video extends Component {

    public Video() {
        setAutoplay(false);
        setControls(true);
        setLoop(false);
        setMuted(false);
        setWidth("320px");
        setHeight("240px");
    }

    public Video(String src) {
        this();
        setSrc(src);
    }

    public Video(StreamResource resource) {
        this();
        setSrc(resource);
    }

    /**
     * Sets the video source as a URL or file path.
     */
    public void setSrc(String src) {
        getElement().setAttribute("src", src);
    }

    /**
     * Sets the video source as a Vaadin StreamResource.
     */
    public void setSrc(StreamResource resource) {
        getElement().setAttribute("src", resource);
    }

    /**
     * Gets the current video source.
     */
    public String getSrc() {
        return getElement().getAttribute("src");
    }

    /**
     * Sets the poster image (displayed before playback starts).
     */
    public void setPoster(String posterUrl) {
        getElement().setAttribute("poster", posterUrl);
    }

    public String getPoster() {
        return getElement().getAttribute("poster");
    }

    /**
     * Enables or disables autoplay.
     */
    public void setAutoplay(boolean autoplay) {
        if (autoplay) {
            getElement().setAttribute("autoplay", true);
        } else {
            getElement().removeAttribute("autoplay");
        }
    }

    public boolean isAutoplay() {
        return getElement().hasAttribute("autoplay");
    }

    /**
     * Shows or hides video controls.
     */
    public void setControls(boolean controls) {
        if (controls) {
            getElement().setAttribute("controls", true);
        } else {
            getElement().removeAttribute("controls");
        }
    }

    public boolean hasControls() {
        return getElement().hasAttribute("controls");
    }

    /**
     * Enables or disables loop playback.
     */
    public void setLoop(boolean loop) {
        if (loop) {
            getElement().setAttribute("loop", true);
        } else {
            getElement().removeAttribute("loop");
        }
    }

    public boolean isLoop() {
        return getElement().hasAttribute("loop");
    }

    /**
     * Mutes or unmutes the video by default.
     */
    public void setMuted(boolean muted) {
        if (muted) {
            getElement().setAttribute("muted", true);
        } else {
            getElement().removeAttribute("muted");
        }
    }

    public boolean isMuted() {
        return getElement().hasAttribute("muted");
    }

    /**
     * Sets the width of the video element (e.g. "480px" or "100%").
     */
    public void setWidth(String width) {
        getElement().getStyle().set("width", width);
    }

    /**
     * Sets the height of the video element (e.g. "320px" or "100%").
     */
    public void setHeight(String height) {
        getElement().getStyle().set("height", height);
    }
}
