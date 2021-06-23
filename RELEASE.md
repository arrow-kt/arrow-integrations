## Release flow

## 1. Create a pull request

Prepare a pull request with these changes:

1. Update versions in `gradle.properties`. For instance, the release version will be `0.10.5` and the next SNAPSHOT version will be `0.11.0-SNAPSHOT`:
```
VERSION_NAME=0.11.0-SNAPSHOT
LATEST_VERSION=0.10.5
```
2. Update the rest of versions in `gradle.properties`:
```
ARROW_VERSION=
COMMON_SETUP=
ROOT_PROJECT=
SUB_PROJECT=
PUBLICATION=
ANIMALSNIFFER=
```

When merging that pull request, this thing will happen automatically:

* New RELEASE version will be published for all the Arrow libraries into Sonatype staging repository.

These changes should be done manually now:

* Create a tag with the RELEASE version.
* Create a release associated to that tag.

## 2. Close and release

Then, close and release the Sonatype repository to sync with Maven Central:

1. Login to https://oss.sonatype.org/ > `Staging repositories`
2. Check the content of the new staging repository
3. Select the staging repository and **Close** (it will check if the content meet the requirements)
4. Select the staging repository and **Release** to sync with Maven Central
5. **Drop** and repeat if there are issues.
