package com.hedera.services.yahcli.commands.files;

/*-
 * ‌
 * Hedera Services Test Clients
 * ​
 * Copyright (C) 2018 - 2021 Hedera Hashgraph, LLC
 * ​
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ‍
 */

import com.hedera.services.yahcli.suites.SysFileDownloadSuite;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

import java.util.concurrent.Callable;

import static com.hedera.services.yahcli.config.ConfigUtils.configFrom;

@Command(
		name = "download",
		subcommands = { picocli.CommandLine.HelpCommand.class },
		description = "Download system files")
public class SysFileDownloadCommand implements Callable<Integer> {
	@ParentCommand
	private SysFilesCommand sysFilesCommand;

	@CommandLine.Option(names = { "-d", "--dest-dir" },
			paramLabel = "destination directory",
			defaultValue = "{network}/sysfiles/")
	private String destDir;

	@Parameters(
			arity = "1..*",
			paramLabel = "<sysfiles>",
			description = "one or more from " +
					"{ address-book, node-details, fees, rates, props, permissions, throttles } (or " +
					"{ 101, 102, 111, 112, 121, 122, 123 })---or 'all'")
	private String[] sysFiles;

	@Override
	public Integer call() throws Exception {
		var config = configFrom(sysFilesCommand.getYahcli());
		destDir = SysFilesCommand.resolvedDir(destDir, config);

		var delegate = new SysFileDownloadSuite(destDir, config.asSpecConfig(), sysFiles);
		delegate.runSuiteSync();

		return 0;
	}

}
