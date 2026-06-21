package backend.dbshared

import io.getquill.MappedEncoding


object MappedEncodingWithCrushIfCorrupted {
  def apply[DbValue, DomainValue, CreationErr](
                                                create: DbValue => Either[CreationErr, DomainValue],
                                                rawValueForLog: DbValue => String = (raw: DbValue) => raw.toString,
                                                creationErrForLog: CreationErr => String = (err: CreationErr) => err.toString
                                              ): MappedEncoding[DbValue, DomainValue] = {
    MappedEncoding[DbValue, DomainValue](
      raw => create(raw).fold(
        err => throw CorruptedDbValueException[DbValue, DomainValue, CreationErr](
          raw, err,
          rawValueForLog = rawValueForLog,
          creationErrForLog = creationErrForLog
        ),
        validValue => validValue
      )
    )
  }
}