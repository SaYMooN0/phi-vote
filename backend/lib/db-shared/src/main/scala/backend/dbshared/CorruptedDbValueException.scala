package backend.dbshared

final class CorruptedDbValueException[DbValue, DomainValue, CreationErr](
                                                                          val rawValue: DbValue,
                                                                          val creationErr: CreationErr,
                                                                          rawValueForLog: DbValue => String,
                                                                          creationErrForLog: CreationErr => String
                                                                        ) extends RuntimeException(
  s"Corrupted DB value while decoding DB value into domain value. " +
    s"Raw value: '${rawValueForLog(rawValue)}'. " +
    s"Creation error: ${creationErrForLog(creationErr)}"
)