package just.semver

import just.Common

import just.fp.syntax._

import just.semver.AdditionalInfo.{BuildMetaInfo, PreRelease}
import just.semver.SemVer.{Major, Minor, Patch}

import scala.util.matching.Regex

/**
 * @author Kevin Lee
 * @since 2018-10-21
 */
final case class SemVer(
    major: Major
  , minor: Minor
  , patch: Patch
  , pre: Option[PreRelease]
  , buildMetadata: Option[BuildMetaInfo]
  ) extends SequenceBasedVersion[SemVer] {

  override def compare(that: SemVer): Int = {
    val mj = this.major.major.compareTo(that.major.major)
    if (mj === 0) {
      val mn = this.minor.minor.compareTo(that.minor.minor)
      if (mn === 0) {
        val pt = this.patch.patch.compareTo(that.patch.patch)
        if (pt === 0) {
          (this.pre, that.pre) match {
            case (Some(thisPre), Some(thatPre)) =>
              Common.compareElems(thisPre.identifier, thatPre.identifier)
            case (Some(_), None) =>
              -1
            case (None, Some(_)) =>
              1
            case (None, None) =>
              0
          }
        } else {
          pt
        }
      } else {
        mn
      }
    } else {
      mj
    }
  }

  def render: String =
    s"${major.major.toString}.${minor.minor.toString}.${patch.patch.toString}" + (
      (pre, buildMetadata) match {
        case (Some(p), Some(m)) =>
          s"-${PreRelease.render(p)}+${BuildMetaInfo.render(m)}"
        case (Some(p), None) =>
          s"-${PreRelease.render(p)}"
        case (None, Some(m)) =>
          s"+${BuildMetaInfo.render(m)}"
        case (None, None) =>
          ""
      }
      ).toString
}

object SemVer {

  final case class Major(major: Int) extends AnyVal
  final case class Minor(minor: Int) extends AnyVal
  final case class Patch(patch: Int) extends AnyVal

  val major0: Major = Major(0)
  val minor0: Minor = Minor(0)
  val patch0: Patch = Patch(0)

  val semVerRegex: Regex =
    """(\d+)\.(\d+)\.(\d+)(?:-([a-zA-Z\d-\.]+)?)?(?:\+([a-zA-Z\d-\.]+)?)?""".r

  def parseUnsafe(version: String): SemVer =
    parse(version) match {
      case Right(semVer) =>
        semVer
      case Left(error) =>
        sys.error(ParseError.render(error))
    }

  def parse(version: String): Either[ParseError, SemVer] = version match {
    case semVerRegex(major, minor, patch, pre, meta) =>
      val preRelease = AdditionalInfo.parsePreRelease(pre)
      val metaInfo = AdditionalInfo.parseBuildMetaInfo(meta)
      (preRelease, metaInfo) match {
        case (Left(preError), Left(metaError)) =>
          Left(ParseError.combine(preError, metaError))
        case (Left(preError), _) =>
          Left(ParseError.preReleaseParseError(preError))
        case (_, Left(metaError)) =>
          Left(ParseError.buildMetadataParseError(metaError))
        case (Right(preR), Right(metaI)) =>
          Right(
            SemVer(
              Major(major.toInt), Minor(minor.toInt), Patch(patch.toInt),
              preR, metaI
            )
          )
      }

    case _ =>
      Left(ParseError.invalidVersionStringError(version))
  }

  def noIdentifier(major: Major, minor: Minor, patch: Patch): SemVer =
    SemVer(major, minor, patch, None, None)

  def withMajor(major: Major): SemVer =
    SemVer(major, minor0, patch0, None, None)

  def withMinor(minor: Minor): SemVer =
    SemVer(major0, minor, patch0, None, None)

  def withPatch(patch: Patch): SemVer =
    SemVer(major0, minor0, patch, None, None)

}