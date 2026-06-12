package backend.shared

object ServiceInfo {
  def health(serviceName: String): String =
    s"$serviceName is alive"
}