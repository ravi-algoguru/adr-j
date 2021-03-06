package org.doble.adr;

import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CommandNewSupercedesTest {
	final static private String rootPathName = "/project/adr";
	final static private String docsPath = "/doc/adr";

	final private String[] adrTitles = {"Another test architecture decision",
			"Yet another test architecture decision",
			"and still the adrs come",
			"to be superseded",
			"some functional name",
			"something to link to",
			"a very important decision"};

	private static FileSystem fileSystem;

	private ADR adr;

	@BeforeEach
	public void setUp() throws Exception {
		Path rootPath = null;

		// Set up the mock file system
		fileSystem = Jimfs.newFileSystem(Configuration.unix());

		rootPath = fileSystem.getPath("/project");

		Files.createDirectory(rootPath);

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

		// Now create a set of files that we can use for the tests
		//for (String adrTitle: adrTitles) {
		for (String adrTitle : adrTitles) {
			// Convert the name to an array of args - including the command.
			args = ("new" + " " + adrTitle).split(" ");
			adr.run(args);
		}
	}

	@AfterEach
	public void tearDown() throws Exception {
		fileSystem.close();
	}

	@Test
	@Order(1)
	public void test1Superseded() throws Exception {
		int[] supercededIds = {5};
		checkSupersedes(supercededIds);
	}

	@Test
	@Order(2)
	public void test2MultipleSupersedes() throws Exception {
		int[] supercededIds = {5, 6, 8};
		checkSupersedes(supercededIds);
	}

	@Test
	@Order(3)
	public void test3SupercedesInvalidADR() throws Exception {

		// badly formed id 
		String[] adrIds = {"foo"};
		assertThrows(ADRException.class, () -> {
			checkSupersedes(adrIds);
		});

		// Non existing adr
		String[] neAdrIds = {"100"};
		assertThrows(ADRException.class, () -> {
			checkSupersedes(neAdrIds);
		});
	}

	public void checkSupersedes(int[] supercededIds) throws Exception {
		String[] strings = new String[supercededIds.length];

		for (int i = 0; i < supercededIds.length; i++) {
			strings[i] = Integer.toString(supercededIds[i]);
		}

		checkSupersedes(strings);
	}

	public void checkSupersedes(String[] supercededIds) throws Exception {
//		int[] supercededIds = Arrays.stream(supercededIdStrings).mapToInt(Integer::parseInt).toArray();
//		
//		// Checks to ensure the integrity of the test data
//		OptionalInt highest = Arrays.stream(supercededIds).max();
//	    assertTrue(highest.getAsInt() < adrTitles.length + 2);
//	    OptionalInt lowest = Arrays.stream(supercededIds).min();
//	    assertTrue(lowest.getAsInt() > 1);  //i.e not the initial ADR or negative

		// Now create a new ADR that supersedes a number of ADRs
		String newADRTitle = "This superceeds number";
		for (String index : supercededIds) {
			newADRTitle += " " + index;
		}

		// Create a new ADR that supersedes others 
		ArrayList<String> argList = new ArrayList<String>();
		argList.add("new");
		for (String id : supercededIds) {
			argList.add("-s");
			argList.add(id);
		}
		argList.addAll(new ArrayList<String>(Arrays.asList((newADRTitle).split(" "))));

		String[] args = {};
		args = argList.toArray(args);
		adr.run(args);

		// Check that the the new record mentions that it supersedes ADR the ids
		int newADRID = adrTitles.length + 2;
		String newADRFileName = TestUtilities.adrFileName(newADRID, newADRTitle);
		Path newADRFile = fileSystem.getPath(rootPathName, docsPath, newADRFileName);

		for (String supersededADRID : supercededIds) {
			long count = 0;
			String title = adrTitles[(new Integer(supersededADRID)).intValue() - 2];
			String supersededADRFileName = TestUtilities.adrFileName(supersededADRID, title);
			String link = "Supersedes the [architecture decision record " + supersededADRID + "](" + supersededADRFileName + ")";
			count = TestUtilities.findString(link, newADRFile);

			assertTrue(count == 1, "The new ADR does not reference the superseded ADR [" + supersededADRID + "] in the text.");
		}

		// Check that the superseded ADRs reference the ADR that supersedes them 
		for (String supersededADRID : supercededIds) {
			long count = 0;
			String title = adrTitles[(new Integer(supersededADRID)).intValue() - 2];
			String supersededADRFileName = TestUtilities.adrFileName(supersededADRID, title);
			Path supersededADRFile = fileSystem.getPath("/project/adr/doc/adr/", supersededADRFileName);
			String link = "Superseded by the [architecture decision record " + newADRID + "](" + newADRFileName + ")";
			count = TestUtilities.findString(link, supersededADRFile);
			assertTrue(count == 1, "The superseded ADR does not reference the  (new) ADR [" + supersededADRID + "] that supersedes it in the text.");
		}
	}
}
