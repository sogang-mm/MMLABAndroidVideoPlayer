package kr.ac.sogang.mmlab.AndroidVideoPlayer.feature.update;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Contains information about a update found by the {@link AppUpdateManager}
 */
@SuppressWarnings("unused")
public final class UpdateInfo implements Serializable
{
    /**
     * version tag of the update
     */
    @NonNull
    private final String versionTag;

    /**
     * title of the update
     */
    @NonNull
    private final String updateTitle;

    /**
     * description of the update
     */
    @NonNull
    private final String updateDesc;

    /**
     * url to the update release on github
     */
    @NonNull
    private final String webUrl;

    /**
     * is this update a pre- release?
     */
    private final boolean isPrerelease;

    /**
     * assets of this update (only apks)
     */
    @NonNull
    private final ApkInfo[] updateAssets;

    public UpdateInfo(@NonNull String versionTag, @NonNull String updateTitle, @NonNull String updateDesc, @NonNull String webUrl, boolean isPrerelease, @NonNull ApkInfo[] updateAssets)
    {
        this.versionTag = versionTag;
        this.updateTitle = updateTitle;
        this.updateDesc = updateDesc;
        this.webUrl = webUrl;
        this.isPrerelease = isPrerelease;
        this.updateAssets = updateAssets;
    }

    /**
     * @return version tag of the update
     */
    @NonNull
    public String getVersionTag()
    {
        return versionTag;
    }

    /**
     * @return title of the update
     */
    @NonNull
    public String getUpdateTitle()
    {
        return updateTitle;
    }

    /**
     * @return description of the update
     */
    @NonNull
    public String getUpdateDesc()
    {
        return updateDesc;
    }

    /**
     * @return url to the update release on github
     */
    @NonNull
    public String getWebUrl()
    {
        return webUrl;
    }

    /**
     * @return is this update a pre- release?
     */
    public boolean isPrerelease()
    {
        return isPrerelease;
    }

    /**
     * @return assets of this update (only apks)
     */
    @NonNull
    public ApkInfo[] getUpdateAssets()
    {
        return updateAssets;
    }

    @Override
    public String toString()
    {
        return "UpdateInfo{" +
                "versionTag='" + versionTag + '\'' +
                ", updateTitle='" + updateTitle + '\'' +
                ", updateDesc='" + updateDesc + '\'' +
                ", webUrl=" + webUrl +
                ", isPrerelease=" + isPrerelease +
                ", updateAssets=" + Arrays.toString(updateAssets) +
                '}';
    }
}
