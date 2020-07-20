/*
 * (c) 2016-2020 Swirlds, Inc.
 *
 * This software is the confidential and proprietary information of
 * Swirlds, Inc. ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Swirlds.
 *
 * SWIRLDS MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF
 * THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
 * TO THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE, OR NON-INFRINGEMENT. SWIRLDS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES.
 */

package com.swirlds.regression;

import com.swirlds.regression.jsonConfigs.RegressionConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TarUtilityTest {

//	@Test
	@DisplayName("Create small tar/gzip")
	void createNewTar(){
		ArrayList<File> fileList = (ArrayList<File>) RegressionUtilities.getSDKFilesToUpload(new File("H:\\Swirlds\\git\\platform-swirlds\\sdk\\testing\\my-key"+".pem"), new File("log4j2.xml"), new ArrayList<File>());
		try (TarGzFile archive = new TarGzFile(Paths.get(new File("tarTest.tar.gz").toURI()))){
			for(File file : fileList){
				if(!file.exists()) { continue; }
				if(file.isFile()){
					archive.bundleFile(Paths.get(file.toURI()));
				}
				else if(file.isDirectory()){
					archive.bundleDirectory(Paths.get(file.toURI()));
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	@DisplayName("Compress results folder")
	void compressResults() {
		ClassLoader classloader = Thread.currentThread().getContextClassLoader();
		URI restartFolder = null;
		try {
			restartFolder = classloader.getResource("results/restart/").toURI();
		} catch (URISyntaxException e){
			e.printStackTrace();
		}
		try (Stream<Path> walkerPath = Files.walk(Paths.get(restartFolder))) {
			CompressResults results = new CompressResults(Paths.get(new File("resultTests.zip").toURI()));
			List<String> foundFiles = walkerPath.map(x -> x.toString()).collect(
					Collectors.toList());
			for (String filename : foundFiles) {
				File file = new File(filename);
				if(!file.exists()) {continue;}
				if(file.isFile()){
					results.bundleFile(Paths.get(file.toURI()));
				}
				else if(file.isDirectory()){
					results.bundleDirectory(Paths.get(file.toURI()));
				}
			}

			results.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
