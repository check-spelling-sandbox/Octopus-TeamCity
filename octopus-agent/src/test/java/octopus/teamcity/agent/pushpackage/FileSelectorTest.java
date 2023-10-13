/*
 * Copyright (c) Octopus Deploy and contributors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 *  these files except in compliance with the License. You may obtain a copy of the
 * License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package octopus.teamcity.agent.pushpackage;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class FileSelectorTest {

  private static final List<String> filesToCreate =
      Lists.newArrayList("firstFile.zip", "secondFile.zip", "third.zip");
  private static final String subDirectoryName = "subDir";
  private static final List<File> rootPathFiles = Lists.newArrayList();
  private static final List<File> subDirectoryFiles = Lists.newArrayList();

  @TempDir static Path testPath;

  @BeforeAll
  public static void setup() throws IOException {
    for (final String filename : filesToCreate) {
      rootPathFiles.add(Files.createFile(testPath.resolve(filename)).toFile());
    }

    final Path subDir = Files.createDirectory(testPath.resolve(subDirectoryName));
    for (final String filename : filesToCreate) {
      subDirectoryFiles.add(Files.createFile(subDir.resolve(filename)).toFile());
    }
  }

  @Test
  public void zipFilesInRootDirectoryAreMatchedButNotSubDir() {
    final Set<File> matchedFiles =
        new FileSelector(testPath).getMatchingFiles(singletonList("*.zip"));
    assertThat(matchedFiles).containsExactlyInAnyOrderElementsOf(rootPathFiles);
  }

  @Test
  public void zipFilesInSubDirAreMatchedButNotRootDir() {
    final Set<File> matchedFiles =
        new FileSelector(testPath.resolve(subDirectoryName))
            .getMatchingFiles(singletonList("*.zip"));
    assertThat(matchedFiles).containsExactlyInAnyOrderElementsOf(subDirectoryFiles);
  }

  @Test
  public void wrongExtensionResultsInNoFiles() {
    final Set<File> matchedFiles =
        new FileSelector(testPath).getMatchingFiles(singletonList("*.tar"));
    assertThat(matchedFiles).isEmpty();
  }

  @Test
  public void exactMatchReturnsOnlyOneFile() {
    final Set<File> matchedFiles =
        new FileSelector(testPath).getMatchingFiles(singletonList(filesToCreate.get(0)));
    assertThat(matchedFiles.size()).isOne();
    final File matchedFile = matchedFiles.iterator().next();
    assertThat(matchedFile.getName()).isEqualTo(filesToCreate.get(0));
  }

  @ParameterizedTest
  @ValueSource(strings = {"/", "\\"})
  public void canMatchOnMultipleEntries(final String delimiterUsedByUser) {
    final Set<File> matchedFiles =
        new FileSelector(testPath)
            .getMatchingFiles(
                Lists.newArrayList("*.zip", subDirectoryName + delimiterUsedByUser + "*.zip"));
    final List<File> fullList = Lists.newArrayList(rootPathFiles);
    fullList.addAll(subDirectoryFiles);
    assertThat(matchedFiles).containsExactlyInAnyOrderElementsOf(fullList);
  }
}
