package brut.androlib.build_actions;

import brut.androlib.build_actions.BuildAction;

import java.io.File;

public final class BuildManifestAndResourcesAction extends BuildAction {
    private final File mManifest;
    private final File mManifestOriginal;

    public BuildManifestAndResourcesAction(File manifest, File manifestOriginal) {

        this.mManifest = manifest;
        this.mManifestOriginal = manifestOriginal;
    }

    public File getManifest() {
        return mManifest;
    }

    public File getManifestOriginal() {
        return mManifestOriginal;
    }
}
