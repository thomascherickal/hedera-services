# System accounts and files

The Hedera network reserves the first 
[`ledger.numReservedSystemEntities=1000`](../hedera-node/src/main/resources/bootstrap.properties#L37) 
entity numbers for use by the system. 
An account with a number in the reserved range is called a **system account**. 
A file with a number in the reserved range is called a **system file**. 

# Privileged transactions

When a system account is the designated payer for a transaction, there 
are cases in which the network grants special **privileges** to the transaction.

There are two kinds of privileges, 
  1. _Authorization_ - some transaction types, such as `Freeze`, require authorization to submit to the network. All such transactions will be rejected with the status `UNAUTHORIZED` unless they are privileged.
  2. _Waived signing requirements_ - all unprivileged `CryptoUpdate` and `FileUpdate` transactions must be signed with the target entity's key, or they will fail with status `INVALID_SIGNATURE`. The network waives this requirement for certain privileged updates.

This document lists all cases of privileged transactions 
currently recognized by the Hedera network. 

## Authorization privileges

First we consider the four transaction types that always require authorization to execute.

| Account Number | `Freeze` | `SystemDelete` | `SystemUndelete` | `UncheckedSubmit` |
| --- | :---: | :---: | :---: | :---: | 
| [`accounts.treasury=2`](../hedera-node/src/main/resources/bootstrap.properties#L28) | X | X | X | X |
| [`accounts.systemAdmin=50`](../hedera-node/src/main/resources/bootstrap.properties#L23) | X | X | X | X |
| [`accounts.freezeAdmin=58`](../hedera-node/src/main/resources/bootstrap.properties#L22) | X |   |   |   |
| [`accounts.systemDeleteAdmin=59`](../hedera-node/src/main/resources/bootstrap.properties#L24) |   | X |   |   |
| [`accounts.systemUndeleteAdmin=60`](../hedera-node/src/main/resources/bootstrap.properties#L24) |   |   | X |   |

Next we consider `FileUpdate` and `FileAppend` transactions when targeting one of the system files.

| Account Number | [`files.addressBook=101`](../hedera-node/src/main/resources/bootstrap.properties#L29)/[`files.nodeDetails=102`](../hedera-node/src/main/resources/bootstrap.properties#L35) | [`files.networkProperties=121`](../hedera-node/src/main/resources/bootstrap.properties#L31)/[`files.hapiPermissions=122`](../hedera-node/src/main/resources/bootstrap.properties#L34)| [`files.feeSchedules=111`](../hedera-node/src/main/resources/bootstrap.properties#L33) | [`files.exchangeRates=112`](../hedera-node/src/main/resources/bootstrap.properties#L32)|
| --- | :---: | :---: | :---: | :---: | 
| [`accounts.treasury=2`](../hedera-node/src/main/resources/bootstrap.properties#L28) | X | X | X | X |
| [`accounts.systemAdmin=50`](../hedera-node/src/main/resources/bootstrap.properties#L23) | X | X | X | X |
| [`accounts.feeSchedulesAdmin=56`](../hedera-node/src/main/resources/bootstrap.properties#L21) |   |   | X |   |
| [`accounts.exchangeRatesAdmin=57`](../hedera-node/src/main/resources/bootstrap.properties#L20) |   | X |   | X |

