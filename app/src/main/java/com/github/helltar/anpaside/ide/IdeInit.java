package com.github.helltar.anpaside.ide;

import static com.github.helltar.anpaside.Consts.ASSET_DIR_FILES;
import static com.github.helltar.anpaside.Consts.ASSET_DIR_STUBS;
import static com.github.helltar.anpaside.Consts.DATA_PKG_PATH;

import android.content.res.AssetManager;

import com.github.helltar.anpaside.logging.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class IdeInit {

    private final AssetManager assetManager;

    public IdeInit(AssetManager assetManager) {
        this.assetManager = assetManager;
    }

    public boolean install() {
        if (copyAssets(ASSET_DIR_STUBS)) {
            return copyAssets(ASSET_DIR_FILES);
        }

        return false;
    }

    private boolean copyAssets(String assetDir) {
        try {
            String[] assets = assetManager.list(assetDir);

            if (assets.length > 0) {
                File dir = new File(DATA_PKG_PATH + assetDir);

                if (!dir.exists()) {
                    dir.mkdir();
                }

                for (String asset : assets) {
                    copyAssets(assetDir + "/" + asset);
                }
            } else {
                InputStream in = assetManager.open(assetDir);
                OutputStream out = new FileOutputStream(DATA_PKG_PATH + assetDir);

                byte[] buf = new byte[1024];
                int len;

                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }

                in.close();
                out.close();
            }

            return true;

        } catch (IOException ioe) {
            Logger.addLog(ioe);
        }

        return false;
    }
}
