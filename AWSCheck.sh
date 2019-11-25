#!/usr/bin/env bash
cd "`dirname "$0"`"

export aws_access_key_id=AKIAJJST62PF5EO3CMAQ
export aws_secret_access_key=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
java \
-Dlog4j.configurationFile=log4j2-jrs.xml \
-Dspring.output.ansi.enabled=ALWAYS \
-Daws.accessKeyId=AKIAJJST62PF5EO3CMAQ \
-Daws.secretKey=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 \
-Dlog4j.configurationFile=log4j2-jrs.xml \
-Dspring.output.ansi.enabled=ALWAYS \
-cp regression.jar com.swirlds.regression.AWSServerCheck \
xoxp-344480056389-344925970834-610132896599-fb69be9200db37ce0b0d55a852b2a5dc \
eng-dev-chat