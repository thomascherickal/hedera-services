module swirlds.regression {
	requires swirlds.common;

	requires com.fasterxml.jackson.core;
	requires com.fasterxml.jackson.databind;

	requires org.apache.logging.log4j;
	requires org.apache.logging.log4j.core;

	requires org.apache.commons.io;
	requires org.apache.commons.compress;
	requires commons.lang3;
	requires httpclient;

	requires org.apache.httpcomponents.httpcore;

	requires aws.java.sdk.ec2;
	requires aws.java.sdk.core;
	requires aws.java.sdk.pricing;

	requires jdk.xml.dom;

	requires org.junit.jupiter.api;
	requires org.junit.jupiter.params;

	requires sshj;

	requires slack.base;
	requires slack.java.client;
	requires algebra;
	requires com.google.guice;

	exports com.swirlds.regression.jsonConfigs;
	exports com.swirlds.regression.jsonConfigs.runTypeConfigs;
	exports com.swirlds.regression.validators;
	exports com.swirlds.regression.slack;

}