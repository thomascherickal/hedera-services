package com.swirlds.regression.validators;

import org.junit.jupiter.api.Test;

import java.util.List;

import static com.swirlds.regression.validators.StreamType.EVENT;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StreamSigFileTest {

	@Test
	public void comparatorTest() {
		StreamSigFile streamSigFile1 = new StreamSigFile("2020-01-07T17_27_00.001157Z.evts_sig", EVENT);
		StreamSigFile streamSigFile2 = new StreamSigFile("2020-01-07T17_25_06.273650Z.evts_sig", EVENT);
		assertTrue(streamSigFile1.compareTo(streamSigFile2) > 0);
	}

	@Test
	public void eventSigEventEqualTest() {
		List<String> evtsSigFiles1 = List.of(
				"2020-01-07T17_23_00.006393Z.evts_sig",
				"2020-01-07T17_24_00.009478Z.evts_sig",
				"2020-01-07T17_27_00.001157Z.evts_sig",
				"2020-01-07T17_25_06.273650Z.evts_sig",
				"2020-01-07T17_32_00.027093Z.evts_sig",
				"2020-01-07T17_22_59.626108Z.evts_sig",
				"2020-01-07T17_31_04.166928Z.evts_sig",
				"2020-01-07T17_29_00.298504Z.evts_sig",
				"2020-01-07T17_30_00.351916Z.evts_sig",
				"2020-01-07T17_28_01.472948Z.evts_sig",
				"2020-01-07T17_26_00.926656Z.evts_sig");
		StreamSigsInANode streamSigsInANode1 = new StreamSigsInANode(evtsSigFiles1, EVENT);

		List<String> evtsSigFiles2 = List.of(
				"2020-01-07T17_23_00.006393Z.evts_sig",
				"2020-01-07T17_24_00.009478Z.evts_sig",
				"2020-01-07T17_27_00.001157Z.evts_sig",
				"2020-01-07T17_25_06.273650Z.evts_sig",
				"2020-01-07T17_22_59.626108Z.evts_sig",
				"2020-01-07T17_31_04.166928Z.evts_sig",
				"2020-01-07T17_29_00.298504Z.evts_sig",
				"2020-01-07T17_30_00.351916Z.evts_sig",
				"2020-01-07T17_28_01.472948Z.evts_sig",
				"2020-01-07T17_26_00.926656Z.evts_sig");
		StreamSigsInANode streamSigsInANode2 = new StreamSigsInANode(evtsSigFiles2, EVENT);
		assertTrue(streamSigsInANode1.equals(streamSigsInANode2));

//		System.out.println(eventSigEvent1);
//		System.out.println("______");
//		System.out.println(eventSigEvent2);
	}
}
