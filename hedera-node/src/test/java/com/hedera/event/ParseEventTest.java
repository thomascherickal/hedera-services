package com.hedera.services.fees;

/*-
 * ‌
 * Hedera Services Node
 * ​
 * Copyright (C) 2018 - 2020 Hedera Hashgraph, LLC
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

import com.swirlds.common.io.SerializableDataInputStream;
import com.swirlds.platform.EventImpl;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

@RunWith(JUnitPlatform.class)
class ParseEventTest {

	private static final String eventFileName = "src/test/resources/testfiles/event/2020-09-04T14_04_00.002272Z.evts";
	@Test
	public void readEventFile() {
		try {
			File file = new File(eventFileName);
			FileInputStream stream = new FileInputStream(file);
			SerializableDataInputStream dis = new SerializableDataInputStream(stream);
			int streamFileVersion = dis.readInt(); // should be 3
			final byte type_prev_hash = dis.readByte(); // should be 1
			byte[] localFileHash = new byte[48];
			dis.readFully(localFileHash);
			while (dis.available() != 0) {
				final byte noTransMarker = dis.readByte(); //should be 90
				final int streamEventVersion = dis.readInt(); // should be 3
				EventImpl event = dis.readSerializable(true, EventImpl::new);
				com.swirlds.common.crypto.Hash hash = dis.readSerializable(true, com.swirlds.common.crypto.Hash::new);
				event.getBaseEventHashedData().setHash(hash);
				System.out.println(event);
			}//while
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}