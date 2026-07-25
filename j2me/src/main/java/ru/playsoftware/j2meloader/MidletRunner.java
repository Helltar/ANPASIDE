/*
 * Copyright 2025 Helltar
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.playsoftware.j2meloader;

import android.content.Context;
import android.util.DisplayMetrics;

import com.android.dx.command.dexer.Main;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.io.inputstream.ZipInputStream;
import net.lingala.zip4j.model.FileHeader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.jar.JarFile;

import ru.playsoftware.j2meloader.config.Config;
import ru.playsoftware.j2meloader.config.ProfileModel;
import ru.playsoftware.j2meloader.config.ProfilesManager;
import ru.playsoftware.j2meloader.util.FileUtils;
import ru.woesss.j2me.jar.Descriptor;

/**
 * The whole public surface of the embedded runtime: hand it a jar, it converts the midlet
 * and starts it. This replaces the loader's own installer, which downloads jad/jar files,
 * keeps a room database of installed apps and drives a settings screen before every run.
 */
public final class MidletRunner {

	private MidletRunner() {
	}

	/**
	 * Converts the jar and opens it in the emulator activity.
	 *
	 * @param name         directory name for the converted midlet, one per project
	 * @param screenWidth  canvas width the midlet sees, 0 to follow the device proportions,
	 *                     or a negative value to use the native display width
	 * @param screenHeight canvas height the midlet sees, 0 to follow the device proportions,
	 *                     or a negative value to use the native display height
	 * @param showKeyboard whether the on screen keypad is drawn under the canvas
	 */
	public static void run(Context context, File jar, String name,
						   int screenWidth, int screenHeight, boolean showKeyboard) throws IOException {
		File appDir = install(context, jar, name);
		ProfileModel profile = applyProfile(context, name, screenWidth, screenHeight, showKeyboard);
		Config.startApp(context, name, appDir.getPath(), false,
				profile.screenWidth, profile.screenHeight, profile.showKeyboard);
	}

	/**
	 * Dexes the jar into {@code <emulator>/converted/<name>} and makes sure a profile exists,
	 * so that the emulator can start without asking anything.
	 */
	public static File install(Context context, File jar, String name) throws IOException {
		File appDir = new File(Config.getAppDir(), name);
		File tmpDir = new File(Config.getAppDir(), ".tmp");

		FileUtils.deleteDirectory(tmpDir);

		if (!tmpDir.mkdirs()) {
			throw new IOException("Can't create directory: " + tmpDir);
		}

		try {
			// --core-library: midlets implement classes from the java.* namespace themselves
			Main.main(new String[]{
					"--no-optimize",
					"--core-library",
					"--output=" + tmpDir + Config.MIDLET_DEX_FILE,
					jar.getAbsolutePath()
			});
		} catch (Throwable e) {
			throw new IOException("Dexing failed: " + e, e);
		}

		FileUtils.copyFileUsingChannel(jar, new File(tmpDir, Config.MIDLET_RES_FILE));
		readDescriptor(jar).writeTo(new File(tmpDir, Config.MIDLET_MANIFEST_FILE));

		FileUtils.deleteDirectory(appDir);

		if (!tmpDir.renameTo(appDir)) {
			throw new IOException("Can't move '" + tmpDir + "' to '" + appDir + "'");
		}

		return appDir;
	}

	/**
	 * A midlet without a profile sends the emulator to its settings screen, which is not
	 * shipped, so one is always written here - and rewritten on every run, because the
	 * screen size lives in the ide settings and can change between runs.
	 */
	private static ProfileModel applyProfile(Context context, String name,
											 int screenWidth, int screenHeight,
											 boolean showKeyboard) throws IOException {
		File configDir = new File(Config.getConfigsDir(), name);

		if (!configDir.exists() && !configDir.mkdirs()) {
			throw new IOException("Can't create directory: " + configDir);
		}

		ProfileModel profile = ProfilesManager.loadConfig(configDir);

		if (profile == null) {
			profile = new ProfileModel(configDir);
		}

		if (screenWidth == 0 || screenHeight == 0) {
			DisplayMetrics metrics = context.getResources().getDisplayMetrics();
			screenWidth = ProfileModel.DEFAULT_WIDTH;
			screenHeight = Math.round((float) screenWidth * metrics.heightPixels / metrics.widthPixels);
		}

		profile.dir = configDir;
		profile.screenWidth = screenWidth;
		profile.screenHeight = screenHeight;
		profile.showKeyboard = showKeyboard;
		// align the canvas to the top, matching j2me loader's default profile
		profile.screenGravity = 1;
		profile.screenBackgroundColor = 0x101010;
		migrateDefaultKeyboardStyle(profile);

		ProfilesManager.saveConfig(profile);
		return profile;
	}

	private static void migrateDefaultKeyboardStyle(ProfileModel profile) {
		if (profile.vkAlpha != 64
				|| profile.vkBgColor != 0xD0D0D0
				|| profile.vkFgColor != 0x000080
				|| profile.vkBgColorSelected != 0x000080
				|| profile.vkFgColorSelected != 0xFFFFFF
				|| profile.vkOutlineColor != 0xFFFFFF) {
			return;
		}

		profile.vkAlpha = 204;
		profile.vkBgColor = 0x3F444C;
		profile.vkFgColor = 0xD7E3FF;
		profile.vkBgColorSelected = 0x6EA8FE;
		profile.vkFgColorSelected = 0x17202B;
		profile.vkOutlineColor = 0x747B86;
	}

	private static Descriptor readDescriptor(File jar) throws IOException {
		ZipFile zip = new ZipFile(jar);
		FileHeader manifest = zip.getFileHeader(JarFile.MANIFEST_NAME);

		if (manifest == null) {
			throw new IOException("JAR has no " + JarFile.MANIFEST_NAME);
		}

		try (ZipInputStream is = zip.getInputStream(manifest)) {
			ByteArrayOutputStream out = new ByteArrayOutputStream(8192);
			byte[] buf = new byte[4096];
			int read;

			while ((read = is.read(buf)) != -1) {
				out.write(buf, 0, read);
			}

			return new Descriptor(out.toString(), false);
		}
	}
}
