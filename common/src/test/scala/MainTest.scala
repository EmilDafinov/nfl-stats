import com.github.dafutils.nflstats.util.UnitTestSpec

class MainTest extends UnitTestSpec {

  "Main" should {
    "generate slick schema" ignore  {
      slick.codegen.SourceCodeGenerator.main(
        Array(
          "slick.jdbc.MySQLProfile",
          "com.mysql.jdbc.NonRegisteringDriver",
          "jdbc:mysql:///nfl?useSSL=false",
          "common/src/main/scala",
          "com.github.dafutils.nflstats.db",
          "root",
          "password"
        )
      )
    }
  }
}
