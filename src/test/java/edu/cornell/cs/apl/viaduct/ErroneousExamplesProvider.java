package edu.cornell.cs.apl.viaduct;

import java.io.File;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

/**
 * Enumerates the paths of IMP source code files under the "errors" directory. These are programs
 * that contain errors.
 */
public class ErroneousExamplesProvider implements ArgumentsProvider {
  @Override
  public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
    Iterable<File> files = () -> FileUtils.iterateFiles(new File("errors"), null, true);
    return StreamSupport.stream(files.spliterator(), false).map(Arguments::of);
  }
}
