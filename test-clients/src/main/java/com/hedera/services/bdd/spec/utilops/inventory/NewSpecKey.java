package com.hedera.services.bdd.spec.utilops.inventory;

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

import com.google.common.base.MoreObjects;
import com.hedera.services.bdd.spec.utilops.UtilOp;
import com.hederahashgraph.api.proto.java.Key;
import com.hedera.services.legacy.proto.utils.KeyExpansion;
import com.hedera.services.bdd.spec.HapiApiSpec;
import com.hedera.services.bdd.spec.keys.KeyGenerator;
import com.hedera.services.bdd.spec.keys.KeyLabel;
import com.hedera.services.bdd.spec.keys.SigControl;
import org.apache.commons.codec.binary.Hex;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;
import static com.hedera.services.bdd.spec.keys.KeyFactory.KeyType;

public class NewSpecKey extends UtilOp {
	static final Logger log = LogManager.getLogger(NewSpecKey.class);

	private boolean verboseLoggingOn = false;
	private final String name;
	private Optional<KeyType> type = Optional.empty();
	private Optional<SigControl> shape = Optional.empty();
	private Optional<KeyLabel> labels = Optional.empty();
	private Optional<KeyGenerator> generator = Optional.empty();

	public NewSpecKey(String name) {
		this.name = name;
	}

	public NewSpecKey logged() {
		verboseLoggingOn = true;
		return this;
	}

	public NewSpecKey type(KeyType toGen) {
		type = Optional.of(toGen);
		return this;
	}

	public NewSpecKey shape(SigControl control) {
		shape = Optional.of(control);
		return this;
	}

	public NewSpecKey labels(KeyLabel kl) {
		labels = Optional.of(kl);
		return this;
	}

	public NewSpecKey generator(KeyGenerator gen) {
		generator = Optional.of(gen);
		return this;
	}

	@Override
	protected boolean submitOp(HapiApiSpec spec) throws Throwable {
		KeyGenerator keyGen = generator.orElse(KeyExpansion::genSingleEd25519KeyByteEncodePubKey);
		Key key;
		if (shape.isPresent()) {
			if (labels.isPresent()) {
				key = spec.keys().generateSubjectTo(shape.get(), keyGen, labels.get());
			} else {
				key = spec.keys().generateSubjectTo(shape.get(), keyGen);
			}
		} else {
			key = spec.keys().generate(type.orElse(KeyType.SIMPLE), keyGen);
		}
		spec.registry().saveKey(name, key);
		if (verboseLoggingOn) {
			if (type.orElse(KeyType.SIMPLE) == KeyType.SIMPLE) {
				log.info("Created simple '{}' w/ Ed25519 public key {}",
						name,
						Hex.encodeHexString(key.getEd25519().toByteArray()));
			} else {
				log.info("Created a complex key...");
			}
		}
		return false;
	}


	@Override
	protected MoreObjects.ToStringHelper toStringHelper() {
		return super.toStringHelper().add("name", name);
	}
}
