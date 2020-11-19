# System accounts and files

The Hedera network reserves the first [`ledger.numReservedSystemEntities=1000`](../hedera-node/src/main/resources/bootstrap.properties#L37) entity numbers for use by the system. An account with a number in the reserved range is called a **system account**. A file with a number in the reserved range is called a **system file**. 

# Privileged transactions

When a system account is the designated payer for a transaction, there are important cases in which the network will grant special **privileges** to the transaction.

There are two kinds of privileges, as follows:
  1. _Authorization_ - some transaction types, such as `Freeze`, require authorization to submit to the network. All such transactions will be rejected with the status `UNAUTHORIZED` unless they are privileged.
  2. _Waived signing requirements_ - all unprivileged `CryptoUpdate` and `FileUpdate` transactions must be signed with the target entity's key, or they will fail with status `INVALID_SIGNATURE`. The network waives this requirement for certain privileged updates.

We now describe all cases of privileged transactions recognized by the Hedera network. 

## Authorization privileges

