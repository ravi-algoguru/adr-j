package org.doble.adr;

import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CommandNewTest {
	private static FileSystem fileSystem;
	final static private String rootPathName = "/project/adr";
	final static private String docsPath = "/doc/adr";

	private String[] adrTitles = {"another test architecture decision",
			"yet another test architecture decision",
			"and still the adrs come",
			"to be superseded",
			"some functional name",
			"something to link to",
			"a very important decision"};

	private ADR adr;

	@BeforeEach
	public void setUp() throws Exception {
		Path rootPath = null;

		// Set up the mock file system
		fileSystem = Jimfs.newFileSystem(Configuration.unix());

		rootPath = fileSystem.getPath("/project");

		Files.createDirectory(rootPath);

		// Set up the environment
		adr = new ADR(new Environment.Builder(fileSystem)
				.out(System.out)
				.err(System.err)
				.in(System.in)
				.userDir(rootPathName)
				.editorCommand("dummyEditor")
				.editorRunner(new TestEditorRunner())
				.build()
		);

		// Set up the directory structure
		String[] args = {"init"};
		adr.run(args);
	}

	@AfterEach
	public void tearDown() throws Exception {
		fileSystem.close();
	}

	@Test
	public void testSimpleCommand() throws Exception {
		String adrTitle = "This is a test achitecture decision";

		String[] args = TestUtilities.argify("new " + adrTitle);

		adr.run(args);

		// Check if the ADR file has been created
		assertTrue(Files.exists(fileSystem.getPath("/project/adr/doc/adr/0002-this-is-a-test-achitecture-decision.md"))); // ADR id is 2 as the first ADR was setup during init.
	}

	@Test
	public void testWithoutTitle() {
		String[] args = {"new"};
		assertThrows(ADRException.class, () -> {
			adr.run(args);
		});
	}

	@Test
	public void testManyADRs() throws Exception {

		// Create a set of test paths, paths of files that should be have been created
		ArrayList<Path> expectedFiles = new ArrayList<Path>();
		ArrayList<String> expectedFileNames = new ArrayList<String>();

		for (int id = 0; id < adrTitles.length; id++) {
			String name = TestUtilities.adrFileName(id + 2, adrTitles[id]);
			Path path = fileSystem.getPath(rootPathName, docsPath, name);
			expectedFiles.add(path);
			expectedFileNames.add(path.toString());
		}

		// And now add on the ADR created during initialization
		Path initADR = fileSystem.getPath(rootPathName, docsPath, "0001-record-architecture-decisions.md");
		expectedFiles.add(initADR);
		expectedFileNames.add(initADR.toString());

		for (String adrName : adrTitles) {
			// Convert the name to an array of args - including the command.
			String[] args = ("new" + " " + adrName).split(" ");
			adr.run(args);
		}

		// Check to see if the names exist 
		Path docsDir = fileSystem.getPath(rootPathName, docsPath);

		Stream<String> actualFileNamesStream = Files.list(docsDir).map(Path::toString);
		List<String> actualFileNames = actualFileNamesStream.collect(Collectors.toList());

		assertTrue(actualFileNames.containsAll(expectedFileNames), "File(s) missing");
		assertTrue(expectedFileNames.containsAll(actualFileNames), "Unexpected file(s) found");
	}
}
