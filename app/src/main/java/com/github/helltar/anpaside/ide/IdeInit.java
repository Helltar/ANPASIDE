package com.github.helltar.anpaside.ide;

import android.content.res.AssetManager;
import com.github.helltar.anpaside.logging.Logger;
import com.github.helltar.anpaside.Utils;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;

import static com.github.helltar.anpaside.Consts.*;

public class IdeInit {

    private AssetManager assetManager;

    public IdeInit(AssetManager assetManager) {
        this.assetManager = assetManager;
    }

    public boolean install() {
        if (copyAssets(ASSET_DIR_BIN)) {
            if (copyAssets(ASSET_DIR_STUBS)) {
                if (copyAssets(ASSET_DIR_FILES)) {
                    // TODO: bool
                    Utils.runProc("chmod 755 " + DATA_PKG_PATH + ASSET_DIR_BIN + "/" + MP3CC);
                    return true;
                }
            }
        }

        return false;
    }

    private boolean copyAssets(String assetDir) {
        try {
            String[] assets = assetManager.list(assetDir);

            if (assets.length > 0) {
                File dir = new File(DATA_PKG_PATH + assetDir);

                if (!(dir.exists())) {
                    dir.mkdir();
                }

                for (int i = 0; i < assets.length; i++) {
                    copyAssets(assetDir + "/" + assets[i]);
                }
            } else {
                FileUtils.copyInputStreamToFile(assetManager.open(assetDir), 
                                                new File(DATA_PKG_PATH + assetDir));
            }

            return true;

        } catch (IOException ioe) {
            Logger.addLog(ioe);
        }

        return false;
    }
}

