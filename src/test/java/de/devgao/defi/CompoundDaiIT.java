// package devgao.io;
//
// import devgao.io.exceptions.MedianIsZeroException;
// import devgao.io.util.GasPriceManager;
// import devgao.io.util.JavaProperties;
// import devgao.io.util.Logger;
// import devgao.io.util.Balances;
// import devgao.io.users.CompoundDai;
// import devgao.io.users.MedianEthereumPrice;
// import org.junit.jupiter.api.BeforeAll;
// import org.junit.jupiter.api.Test;
// import org.web3j.protocol.Web3j;
// import org.web3j.protocol.http.HttpService;
//
// import java.math.BigDecimal;
// import java.math.BigInteger;
//
// import static devgao.io.util.Logger.getLogFilePath;
// import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
// import static org.junit.jupiter.api.Assertions.assertEquals;
//
// class CompoundDaiIT {
//    @Test
//    void mintAndRedeemTest() {
//        assertDoesNotThrow(() -> {
//            assertEquals(BigDecimal.ZERO, Balances.getDaiInCompound());
//            CompoundDai.mint(new BigInteger("5000000000000000000"),
// MedianEthereumPrice.getPrice());
//            // org.opentest4j.AssertionFailedError: Unexpected exception thrown:
// java.lang.AssertionError: expected:<5000000000000000000> but was:<4999999999977162310>
//            assertEquals(new BigDecimal("5000000000000000000"), Balances.getDaiInCompound());
//            CompoundDai.redeem(new BigInteger("5000000000000000000"), BigDecimal.ZERO,
// MedianEthereumPrice.getPrice());
//            assertEquals(BigDecimal.ZERO, Balances.getDaiInCompound());
//        });
//    }
//
//    @Test
//    void redeemTest() throws MedianIsZeroException {
//        CompoundDai.redeem(new BigInteger("5000000000000000000"), BigDecimal.ZERO,
// MedianEthereumPrice.getPrice()); // too much? 0.000000024445745446 for 244.45745446
//        assertEquals(BigDecimal.ZERO, Balances.getDaiInCompound());
//    }
//
//    @Test
//    void redeemAllTest() throws MedianIsZeroException {
//        CompoundDai.redeemAll(BigDecimal.ZERO, MedianEthereumPrice.getPrice());
//        assertEquals(BigInteger.ZERO, Balances.getCdai());
//    }
//
//    @Test
//    void getCDaiBalanceTest() {
//        BigInteger cDaiBalance = CompoundDai.getCDaiBalance();
//        assertEquals(BigInteger.ZERO, cDaiBalance);
//    }
//
//    @BeforeAll
//    static void init() throws Exception {
//        // set log file path
//        String currentDirectory = getLogFilePath();
//
//        // create a new web3j instance to connect to remote nodes on the network
//        GasPriceManager.web3j = Web3j.build(new
// HttpService("https://mainnet.infura.io/v3/8f77571f778f4c7a95f735857a5a340f"));
//        Logger.log("Connected to Ethereum client version: " +
// GasPriceManager.web3j.web3ClientVersion().send().getWeb3ClientVersion());
//
//        // load Environment variables
//        JavaProperties javaProperties = new JavaProperties(currentDirectory);
//
//        // initialize persistent scripts
//        GasPriceManager.initializePersistentScripts();
//
//        // check if interacting with persistent scripts is allowed
//        GasPriceManager.checkAllowances();
//    }
// }
