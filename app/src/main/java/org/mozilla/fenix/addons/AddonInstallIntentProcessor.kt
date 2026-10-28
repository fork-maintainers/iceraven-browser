import android.content.Context
import android.content.Intent
import android.net.Uri
import mozilla.components.concept.engine.webextension.InstallationMethod
import mozilla.components.concept.engine.webextension.WebExtension
import mozilla.components.concept.engine.webextension.WebExtensionRuntime
import mozilla.components.feature.intent.processing.IntentProcessor
import mozilla.components.support.base.log.logger.Logger
import mozilla.components.support.ktx.android.net.getFileName
import mozilla.components.support.utils.toSafeIntent
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException


class AddonInstallIntentProcessor(private val context: Context, private val runtime: WebExtensionRuntime) : IntentProcessor {
    private fun matches(intent: Intent) =
        intent.data != null
                && intent.scheme.equals("content")
                && intent.type in arrayOf("application/x-xpinstall", "application/zip")

    private val logger = Logger("AddonInstallIntentProcessor")

    override fun process(intent: Intent): Boolean {
        val safeIntent = intent.toSafeIntent()
        val url = safeIntent.dataString

        return if (!url.isNullOrEmpty() && matches(intent)) {
            val uriData = intent.data as Uri
            try {
                val file = fromUri(uriData)
                val extURI = parseExtension(file)
                installExtension(extURI) {}
            } catch (e: IOException) {
                // The sending app gave us a URI we can't read; there is nothing to install.
                logger.error("Could not copy the add-on file", e)
            } catch (e: SecurityException) {
                logger.error("Not allowed to read the add-on file", e)
            }
            true
        } else {
            false
        }
    }

    fun installExtension(b64: String, onSuccess: ((WebExtension) -> Unit)) {
        runtime.installWebExtension(b64, InstallationMethod.FROM_FILE, onSuccess)
    }

    fun parseExtension(inp: File): String {
        return Uri.fromFile(inp.absoluteFile).toString()
    }

    fun fromUri(uri: Uri): File {
        val name = uri.getFileName(context.contentResolver)
        val file = File(context.externalCacheDir ?: context.cacheDir, name)
        val istream = context.contentResolver.openInputStream(uri)
            ?: throw FileNotFoundException("Could not open $uri")
        istream.use { input ->
            FileOutputStream(file).use { output -> input.copyTo(output) }
        }
        return file
    }
}
