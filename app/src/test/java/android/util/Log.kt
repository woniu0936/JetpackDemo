package android.util

object Log {
    @JvmStatic
    fun d(tag: String, msg: String): Int {
        println("D/$tag: $msg")
        return 0
    }

    @JvmStatic
    fun w(tag: String, msg: String): Int {
        println("W/$tag: $msg")
        return 0
    }

    @JvmStatic
    fun w(tag: String, tr: Throwable): Int {
        println("W/$tag: ${tr.message}")
        return 0
    }

    @JvmStatic
    fun w(tag: String, msg: String, tr: Throwable): Int {
        println("W/$tag: $msg")
        tr.printStackTrace()
        return 0
    }

    @JvmStatic
    fun e(tag: String, msg: String): Int {
        println("E/$tag: $msg")
        return 0
    }

    @JvmStatic
    fun e(tag: String, msg: String, tr: Throwable): Int {
        println("E/$tag: $msg")
        tr.printStackTrace()
        return 0
    }
}
