# CHANGELOG.md

## 0.14.0 (2022-11-27)

Bugfixes:
- @miguelbaldi Fix `EitherModule` deserialization fails on collections of either
- @myuwono Bugfix: Calling `ObjectMapper().registerArrowModule()` causes NonEmptyList deserialization to fail with NullPointerException
- @i-walker Use arrow-gradle-config plugins and move to build.gradle.kts
- @myuwono update Option codec so to make it honor object mapper configurations, this includes the NON_ABSENT inclusion
- @rachelcarmena Fix release plugins for arrow-integrations 
- @myuwono Fix incorrect jvm compatibility 

Features:

- @myuwono Introduce support for basic Arrow datatypes including Either, Validated, and Ior https://github.com/arrow-kt/arrow-integrations/pull/71
- @myuwono Upgrade Jackson to 2.13.4
- @i-walker Upgrade Arrow to 1.1.3

Contributors:
- @i-walker
- @nomisRev
- @miguelbaldi
- @raulraja
- @rachelcarmena
- @myuwono
- @JavierSegoviaCordoba