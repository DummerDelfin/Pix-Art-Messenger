package de.pixart.messenger.services;

public class MediaPlayer extends android.media.MediaPlayer {
    private int streamType;

    public int getAudioStreamType() {
        return streamType;
    }

    @Override
    public void setAudioStreamType(int streamType) {
        this.streamType = streamType;
        super.setAudioStreamType(streamType);
    }
}