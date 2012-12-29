package net.oddsoftware.android.feedscribe.data;

public interface FeedUpdateListener
{
    public abstract void feedUpdateProgress(int stage, int numStages);
}
