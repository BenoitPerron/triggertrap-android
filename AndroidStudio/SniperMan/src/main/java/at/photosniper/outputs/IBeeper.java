package at.photosniper.outputs;

public interface IBeeper {

    void play(long length);

    void play(long length, long pauseLength);

    void stop();
}
