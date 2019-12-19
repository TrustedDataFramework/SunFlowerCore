/*
 * Copyright (c) [2019] [ <silk chain> ]
 * This file is part of the silk chain library.
 *
 * The silk chain library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The silk chain library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the silk chain library. If not, see <http://www.gnu.org/licenses/>.
 */
package org.tdf.sunflower.vrf.vm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URISyntaxException;

import org.apache.catalina.startup.ClassLoaderFactory.Repository;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.jce.interfaces.ECKey;
import org.spongycastle.util.encoders.Hex;
import org.tdf.sunflower.consensus.poa.config.Genesis;
import org.tdf.sunflower.consensus.vrf.contract.PrecompiledContracts;
import org.tdf.sunflower.consensus.vrf.contract.VrfContracts;
import org.tdf.sunflower.consensus.vrf.db.HashMapDB;
import org.tdf.sunflower.consensus.vrf.vm.DataWord;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.Transaction;

public class CallTxAndExecuteTest {
    /*

	private static final Logger logger = LoggerFactory.getLogger("CallTxAndExecuteTest");

	private static final DataWord contractAddr = DataWord.of("0000000000000000000000000000000000000000000000000000000000000011");

	private BlockchainImpl blockchain;

	private PendingState pendingState;

	private Repository cacheTrack;

	@Before
	public void setup() throws URISyntaxException, IOException, InterruptedException {
		// Check Genesis from ethereumj.conf -> frontier.json
		Genesis genesis = (Genesis)Genesis.getInstance();
		blockchain = createBlockchain(genesis);
		pendingState = ((BlockchainImpl) blockchain).getPendingState();
	}

	@Test
	public void testDepositeOK() {
		final byte[] coinbase = org.spongycastle.util.encoders.Hex.decode("1111ceb4e6f404934d098957d200e803239fdf75");

		final long gasUsedInTheBlock = 100000;

		// Call deposit
		long nonce = 0;
		long gasPrice = 100;
		long gasLimit = 60000;
		byte[] receiveAddress = contractAddr.getData();
		long value = 300000;
		byte[] data = Hex.decode("d0e30db0");

		Transaction tx = new Transaction(longToBytesNoLeadZeroes(nonce),
				longToBytesNoLeadZeroes(gasPrice),
				longToBytesNoLeadZeroes(gasLimit),
				receiveAddress,
				longToBytesNoLeadZeroes(value),
				data, null);
		// put mock signature if not present
		tx.sign(ECKey.DUMMY);

		// Set current block Gas Limit
		Block currentBlock = blockchain.getBestBlock();
		currentBlock.getHeader().setGasLimit(longToBytesNoLeadZeroes(1000000));

		// Set sender balance
		Repository track = pendingState.getRepository();
		track.addBalance(tx.getSender(), BigInteger.valueOf(0x10000000));

		TransactionExecutor executor = new TransactionExecutor(
				tx, coinbase, track, blockchain.getBlockStore(),
				blockchain.getProgramInvokeFactory(), currentBlock, new EthereumListenerAdapter(),
				gasUsedInTheBlock);

		executor.init();
		executor.execute();
		executor.go();
		executor.finalization();

		TransactionReceipt receipt = executor.getReceipt();

		assertEquals(0, receipt.getLogInfoList().size());
		// gasUsedInTheBlock(100000) + Gas used(22872 = 1600 + 21272(basicTxCost))
		long gasRequired = getVrfContractGasRequired(tx, cacheTrack);
		BigInteger cumulativeGas = BigInteger.valueOf(gasUsedInTheBlock + gasRequired + 21272);
		assertTrue(new BigInteger(1, receipt.getCumulativeGas()).compareTo(cumulativeGas) == 0);
		BigInteger gasUsed = BigInteger.valueOf(gasRequired + 21272);
		assertTrue(new BigInteger(1, receipt.getGasUsed()).compareTo(gasUsed) == 0);
		assertTrue(isNullOrZeroArray(receipt.getExecutionResult()));
		assertTrue(receipt.isTxStatusOK());
		assertTrue(receipt.isSuccessful());

		System.out.println("{" + receipt + "}");
	}

	@Test
	public void testDepositeFail() {
		final byte[] coinbase = org.spongycastle.util.encoders.Hex.decode("2222ceb4e6f404934d098957d200e803239fdf75");

		final long gasUsedInTheBlock = 100000;

		// Call deposit
		long nonce = 0;
		long gasPrice = 100;
		long gasLimit = 60000;
		byte[] receiveAddress = contractAddr.getData();
		long value = 0;
		byte[] data = Hex.decode("d0e30db0");

		Transaction tx = new Transaction(longToBytesNoLeadZeroes(nonce),
				longToBytesNoLeadZeroes(gasPrice),
				longToBytesNoLeadZeroes(gasLimit),
				receiveAddress,
				longToBytesNoLeadZeroes(value),
				data, null);
		// put mock signature if not present
		tx.sign(ECKey.DUMMY);

		// Set current block Gas Limit
		Block currentBlock = blockchain.getBestBlock();
		currentBlock.getHeader().setGasLimit(longToBytesNoLeadZeroes(1000000));

		// Set sender balance
		Repository track = pendingState.getRepository();
		track.addBalance(tx.getSender(), BigInteger.valueOf(0x10000000));

		TransactionExecutor executor = new TransactionExecutor(
				tx, coinbase, track, blockchain.getBlockStore(),
				blockchain.getProgramInvokeFactory(), currentBlock, new EthereumListenerAdapter(),
				gasUsedInTheBlock);

		executor.init();
		executor.execute();
		executor.go();
		executor.finalization();

		TransactionReceipt receipt = executor.getReceipt();

		assertEquals(0, receipt.getLogInfoList().size());
		// gasUsedInTheBlock(100000) + Gas Limit(60000))
		BigInteger cumulativeGas = BigInteger.valueOf(gasUsedInTheBlock + gasLimit);
		assertTrue(new BigInteger(1, receipt.getCumulativeGas()).compareTo(cumulativeGas) == 0);
		BigInteger gasUsed = BigInteger.valueOf(gasLimit);
		assertTrue(new BigInteger(1, receipt.getGasUsed()).compareTo(gasUsed) == 0);
		assertTrue(isNullOrZeroArray(receipt.getExecutionResult()));
		assertTrue(receipt.isTxStatusOK() == false);
		assertTrue(receipt.isSuccessful() == false);

		System.out.println("{" + receipt + "}");
	}

	@Test
	public void testSetPubkey() {
		final byte[] coinbase = org.spongycastle.util.encoders.Hex.decode("3333ceb4e6f404934d098957d200e803239fdf75");
		final String pubkey = new String("bbb361229922ff28bd992c5bbe344c7afdef76fd18c796618c9e9ab7ebae65d9");

		final long gasUsedInTheBlock = 100000;

		// Call deposit
		long nonce = 0;
		long gasPrice = 100;
		long gasLimit = 60000;
		byte[] receiveAddress = contractAddr.getData();
		long value = 0;
		// function hash code for setPubkey(bytes32)
		byte[] data = Hex.decode("6e7b26b0" + pubkey);

		Transaction tx = new Transaction(longToBytesNoLeadZeroes(nonce),
				longToBytesNoLeadZeroes(gasPrice),
				longToBytesNoLeadZeroes(gasLimit),
				receiveAddress,
				longToBytesNoLeadZeroes(value),
				data, null);
		// put mock signature if not present
		tx.sign(ECKey.DUMMY);

		// Set current block Gas Limit
		Block currentBlock = blockchain.getBestBlock();
		currentBlock.getHeader().setGasLimit(longToBytesNoLeadZeroes(1000000));

		// Set sender balance
		Repository track = pendingState.getRepository();
		track.addBalance(tx.getSender(), BigInteger.valueOf(0x10000000));

		TransactionExecutor executor = new TransactionExecutor(
				tx, coinbase, track, blockchain.getBlockStore(),
				blockchain.getProgramInvokeFactory(), currentBlock, new EthereumListenerAdapter(),
				gasUsedInTheBlock);

		executor.init();
		executor.execute();
		executor.go();
		executor.finalization();

		TransactionReceipt receipt = executor.getReceipt();

		assertTrue(receipt.isTxStatusOK());
		assertTrue(receipt.isSuccessful());

		++nonce;
		// Call getPubkey
		data = Hex.decode("063cd44f");

		tx = new Transaction(longToBytesNoLeadZeroes(nonce),
				longToBytesNoLeadZeroes(gasPrice),
				longToBytesNoLeadZeroes(gasLimit),
				receiveAddress,
				longToBytesNoLeadZeroes(value),
				data, null);
		// put mock signature if not present
		tx.sign(ECKey.DUMMY);

		executor = new TransactionExecutor(
				tx, coinbase, track, blockchain.getBlockStore(),
				blockchain.getProgramInvokeFactory(), currentBlock, new EthereumListenerAdapter(),
				gasUsedInTheBlock);

		executor.init();
		executor.execute();
		executor.go();
		executor.finalization();

		receipt = executor.getReceipt();

		byte[] result = receipt.getExecutionResult();

		assertTrue(receipt.isTxStatusOK());
		assertTrue(receipt.isSuccessful());
		assertEquals(pubkey, Hex.toHexString(result));

		System.out.println("{" + receipt + "}");
	}

	private BlockchainImpl createBlockchain(Genesis genesis) {
		IndexedBlockStore blockStore = new IndexedBlockStore();
		blockStore.init(new HashMapDB<byte[]>(), new HashMapDB<byte[]>());

		Repository repository = new RepositoryRoot(new HashMapDB());

		ProgramInvokeFactoryImpl programInvokeFactory = new ProgramInvokeFactoryImpl();

		BlockchainImpl blockchain = new BlockchainImpl(blockStore, repository)
				.withParentBlockHeaderValidator(new CommonConfig().parentHeaderValidator());
		blockchain.setParentHeaderValidator(new DependentBlockHeaderRuleAdapter());
		blockchain.setProgramInvokeFactory(programInvokeFactory);

		blockchain.byTest = true;

		PendingStateImpl pendingState = new PendingStateImpl(new EthereumListenerAdapter());

		pendingState.setBlockchain(blockchain);
		blockchain.setPendingState(pendingState);

		this.cacheTrack = repository.startTracking();
		Genesis.populateRepository(this.cacheTrack, genesis);
		
******************************/
		
		/**
		 * Add VRF deposit for Genesis block.
		 *
		 * Allow genesis to make up initial deposit for VRF consensus protocol.
		 *
		 * @author James Hu, silk chain
		 * @since 2019/06/13
		 */
		//genesis.setStateRoot(this.cacheTrack.getRoot());
/**********************************
		this.cacheTrack.commit();

		blockStore.saveBlock(Genesis.getInstance(), Genesis.getInstance().getDifficultyBI(), true);

		blockchain.setBestBlock(Genesis.getInstance());
		blockchain.setTotalDifficulty(Genesis.getInstance().getDifficultyBI());

		return blockchain;
	}

	private long getVrfContractGasRequired(final Transaction tx, Repository cacheTrack) {
		// Try to get VRF precompiled contract with its address
		byte[] targetAddress = tx.getReceiveAddress();
		PrecompiledContracts.PrecompiledContract precompiledContract = VrfContracts.getContractForAddress(
				DataWord.of(targetAddress), tx.getSender(), tx.getValue(), cacheTrack, 0);

		// Calculate Gas to be used
		if (precompiledContract != null) {
			return precompiledContract.getGasForData(tx.getData());
		}

		return 0;
	}
	
	*/
}