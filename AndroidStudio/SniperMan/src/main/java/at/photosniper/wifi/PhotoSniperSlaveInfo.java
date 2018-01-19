package at.photosniper.wifi;

public class PhotoSniperSlaveInfo {

    private String name;
    private String uniqueName;

    public PhotoSniperSlaveInfo(String name, String uniqueName) {
        this.name = name;
        this.uniqueName = uniqueName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUniqueName() {
        return uniqueName;
    }

    public void setUniqueName(String uniqueName) {
        this.uniqueName = uniqueName;
    }


}
