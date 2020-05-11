module com.swirlds.regression {
	requires com.swirlds.common;

	requires com.fasterxml.jackson.core;
	requires com.fasterxml.jackson.databind;

	requires org.apache.logging.log4j;
	requires org.apache.logging.log4j.core;

	requires java.scripting;
	requires java.sql;

	requires org.apache.commons.io;
	requires org.apache.commons.compress;
	requires org.apache.commons.lang3;
	requires httpclient;

	requires org.apache.httpcomponents.httpcore;

	requires aws.java.sdk.ec2;
	requires aws.java.sdk.core;
	requires aws.java.sdk.pricing;
	requires aws.java.sdk.costexplorer;

	requires jdk.xml.dom;

	requires org.junit.jupiter.api;
	requires org.junit.jupiter.params;

	requires sshj;

	requires slack.base;
	requires slack.java.client;
	requires algebra;
	requires com.google.guice;
	requires com.swirlds.fcmap;
	requires com.swirlds.demo.platform;
	requires com.swirlds.platform;
	requires com.swirlds.fcmap.test;

	exports com.swirlds.regression.jsonConfigs;
	exports com.swirlds.regression.jsonConfigs.runTypeConfigs;
	exports com.swirlds.regression.validators;
	exports com.swirlds.regression.slack;
	exports com.swirlds.regression.experiment;

}