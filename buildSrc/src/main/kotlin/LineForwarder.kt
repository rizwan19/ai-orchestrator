import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Re-emits a child process's output line by line, prefixed, from a dedicated thread.
 *
 * When long-running tasks run in parallel (e.g. `devAll`), Gradle buffers output written on task threads and
 * may hold a quiet server's last lines back for ~30 s. Output printed from an unrelated thread shows immediately.
 */
class LineForwarder(private val prefix: String) : OutputStream() {

    private val line = ByteArrayOutputStream()
    private val printer = Executors.newSingleThreadExecutor { Thread(it, "output-$prefix").apply { isDaemon = true } }

    @Synchronized
    override fun write(b: Int) {
        if (b == '\n'.code) emitLine() else line.write(b)
    }

    @Synchronized
    override fun write(bytes: ByteArray, offset: Int, length: Int) {
        for (i in offset until offset + length) write(bytes[i].toInt())
    }

    @Synchronized
    override fun close() {
        if (line.size() > 0) emitLine()
        printer.shutdown()
        printer.awaitTermination(5, TimeUnit.SECONDS)
    }

    private fun emitLine() {
        val text = line.toString(Charsets.UTF_8).trimEnd('\r')
        line.reset()
        printer.execute { println("[$prefix] $text") }
    }
}
