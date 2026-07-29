package com.github.helltar.anpaside.apk

import com.android.apksig.ApkSigner
import com.android.apksig.KeyConfig
import com.github.helltar.anpaside.foundation.createDirectories
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipInputStream

/**
 * A midlet ready to be packed: the files the emulator loads it from, keyed by the name they
 * get inside the apk.
 */
data class ApkExportRequest(
    val application: ApkApplication,
    val midletFiles: Map<String, File>,
    val icon: File?,
    val target: File
)

/**
 * Turns a converted midlet into an installable apk.
 *
 * There is no aapt2 and no gradle here, so nothing is built: the player apk that ships in the
 * ide's assets is rewritten with this midlet's manifest, icon and files, and signed. Everything
 * that needs a real toolchain - resources, the runtime's own dex - was already done when the
 * player module was built.
 */
class ApkExporter(
    private val template: () -> InputStream,
    private val signingKey: ApkSigningKey,
    private val workDirectory: File
) {

    fun export(request: ApkExportRequest): File {
        workDirectory.createDirectories()
        val unsigned = workDirectory.resolve(UNSIGNED_FILE)

        try {
            ApkArchive.repack(
                template = template(),
                target = unsigned,
                replacements = replacements(request),
                additions = request.midletFiles.mapKeys { (name, _) ->
                    ApkTemplate.MIDLET_ASSET_DIRECTORY + name
                }
            )

            request.target.parentFile?.createDirectories()
            sign(unsigned, request.target)
        } finally {
            unsigned.delete()
        }

        return request.target
    }

    private fun replacements(request: ApkExportRequest): Map<String, ByteArray> {
        val manifest =
            readEntry(ApkTemplate.MANIFEST_ENTRY)
                ?: throw IOException("The apk template has no ${ApkTemplate.MANIFEST_ENTRY}")

        val replacements =
            mutableMapOf(
                ApkTemplate.MANIFEST_ENTRY to
                        ApkTemplate.patchManifest(manifest, request.application)
            )

        // a midlet without a readable icon of its own keeps the template's
        request.icon
            ?.takeIf(File::isFile)
            ?.let(LauncherIcon::compose)
            ?.let { icon -> replacements[ApkTemplate.ICON_ENTRY] = icon }

        return replacements
    }

    private fun readEntry(name: String): ByteArray? =
        ZipInputStream(template()).use { input ->
            generateSequence { input.nextEntry }
                .firstOrNull { entry -> entry.name == name }
                ?.let { input.readBytes() }
        }

    private fun sign(unsigned: File, target: File) {
        val identity = signingKey.identity()

        val signer =
            ApkSigner.SignerConfig.Builder(
                SIGNER_NAME,
                KeyConfig.Jca(identity.privateKey),
                listOf(identity.certificate)
            ).build()

        ApkSigner.Builder(listOf(signer))
            .setInputApk(unsigned)
            .setOutputApk(target)
            .setMinSdkVersion(ApkTemplate.MIN_SDK)
            // v2 covers everything the template can be installed on, and skipping v1 saves
            // digesting every entry a second time
            .setV1SigningEnabled(false)
            .setV2SigningEnabled(true)
            .setV3SigningEnabled(true)
            .build()
            .sign()
    }

    private companion object {
        const val UNSIGNED_FILE = "unsigned.apk"
        const val SIGNER_NAME = "anpaside"
    }
}
