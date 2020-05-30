package devgao.io.uniswap;

import devgao.io.compounddai.CompoundDai;
import devgao.io.contractneedsprovider.*;
import devgao.io.dai.Dai;
import devgao.io.gasprovider.GasProvider;
import devgao.io.numberutil.Wad18;
import devgao.io.util.Balances;
import devgao.io.util.Ethereum;
import devgao.io.util.JavaProperties;
import devgao.io.weth.Weth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class UniswapIT {
  private static final String TRAVIS_INFURA_PROJECT_ID = "TRAVIS_INFURA_PROJECT_ID";
  private static final String TRAVIS_WALLET = "TRAVIS_WALLET";
  private static final String TRAVIS_PASSWORD = "TRAVIS_PASSWORD";

  public Uniswap uniswap;
  public Balances balances;
  public ContractNeedsProvider contractNeedsProvider;
  public JavaProperties javaProperties;
  public Weth weth;
  public Ethereum ethereum;
  public CompoundDai compoundDai;
  public Dai dai;

  @BeforeEach
  public void setUp() {
    javaProperties = new JavaProperties(true);

    String infuraProjectId;
    String password;
    String wallet;

    Permissions permissions =
        new Permissions(
            Boolean.parseBoolean(javaProperties.getValue("transactionsRequireConfirmation")),
            Boolean.parseBoolean(javaProperties.getValue("playSoundOnTransaction")));

    if ("true".equals(System.getenv().get("TRAVIS"))) {
      infuraProjectId = System.getenv().get(TRAVIS_INFURA_PROJECT_ID);
      wallet = System.getenv().get(TRAVIS_WALLET);
      password = System.getenv().get(TRAVIS_PASSWORD);
    } else {
      infuraProjectId = javaProperties.getValue("infuraProjectId");
      wallet = javaProperties.getValue("wallet");
      password = javaProperties.getValue("password");
    }

    Web3j web3j = new Web3jProvider(infuraProjectId).web3j;
    GasProvider gasProvider =
            new GasProvider(
                    web3j, new Wad18(1_000000000), new Wad18(1000_000000000L));
    Credentials credentials = new Wallet(password, wallet).getCredentials();
    CircuitBreaker circuitBreaker = new CircuitBreaker();
    contractNeedsProvider =
        new ContractNeedsProvider(web3j, credentials, gasProvider, permissions, circuitBreaker);

    dai =
        new Dai(
            contractNeedsProvider,
            Double.parseDouble(javaProperties.getValue("minimumDaiNecessaryForSaleAndLending")));
    compoundDai = new CompoundDai(contractNeedsProvider);
    weth = new Weth(contractNeedsProvider);
    ethereum =
        new Ethereum(
            contractNeedsProvider,
            Double.parseDouble(javaProperties.getValue("minimumEthereumReserveUpperLimit")),
            Double.parseDouble(javaProperties.getValue("minimumEthereumReserveLowerLimit")),
            Double.parseDouble(javaProperties.getValue("minimumEthereumNecessaryForSale")));

    balances = new Balances(dai, weth, compoundDai, ethereum);
    uniswap = new Uniswap(contractNeedsProvider, javaProperties, compoundDai, weth);
  }

  @Test
  public void getProfitableBuyDaiOffer_someRealNumbers_returnExpectedCalculation() {
    Wad18 buyableDaiAmount =
            new Wad18("4533813969247998520957"); // 4533.813969247998520957
    Wad18 medianEthereumPrice =
            new Wad18("231690000000000000000"); // 231.690000000000000000
    Wad18 ethToSell = new Wad18("19439031735500000000"); // 19.439031735500000000
    UniswapOffer offer =
            uniswap.getProfitableBuyDaiOffer(
                    buyableDaiAmount, ethToSell, balances, medianEthereumPrice, 0.35);
    assertEquals(new Wad18("19490059192502288879"), offer.profit);
  }

  @Test
  public void getBuyDaiParameters_buyableAmountIsZero_Null() throws Exception {
    Wad18 medianEthereumPrice = new Wad18("231690000000000000000");
    EthToTokenSwapInput ethToTokenSwapInput =
            uniswap.getBuyDaiParameters(balances, medianEthereumPrice);
    assertNull(ethToTokenSwapInput);
  }

  @Test
  public void
      getBuyDaiParameters_buyableAmountIsBiggerThanZero_allEthToTokenSwapInputAttributesNonZero()
          throws Exception {
    Wad18 medianEthereumPrice = new Wad18("231690000000000000000");
    EthToTokenSwapInput ethToTokenSwapInput =
            uniswap.getBuyDaiParameters(balances, medianEthereumPrice);
    assertNull(ethToTokenSwapInput);
  }

  @Test
  public void getSellDaiParameters_buyableAmountIsZero_Null() throws IOException {
    // TODO: test should not depend on real balances
    Wad18 medianEthereumPrice = new Wad18("231690000000000000000");
    TokenToEthSwapInput tokenToEthSwapInput =
            uniswap.getSellDaiParameters(balances, medianEthereumPrice);
    assertNull(tokenToEthSwapInput);
  }

  // TODO: use Mockito set eth and weth balances to non-zero and do the following tests

  @Test
  public void
      getSellDaiParameters_buyableAmountIsBiggerThanZero_allTokenToEthSwapInputAttributesNonZero() {}

  @Test
  public void getProfitableBuyDaiOffer_triggerException_uniswapOfferZeroZero() {}

  @Test
  public void getProfitableBuyDaiOffer_lowerThanMinimumProfit_uniswapOfferZeroNonZero() {}

  @Test
  public void getProfitableBuyDaiOffer_higherThanMinimumProfit_uniswapOfferNonZeroNonZero() {}

  @Test
  public void getProfitableSellDaiOffer_triggerException_uniswapOfferZeroZero() {}

  @Test
  public void getProfitableSellDaiOffer_lowerThanMinimumProfit_uniswapOfferZeroNonZero() {}

  @Test
  public void getProfitableSellDaiOffer_higherThanMinimumProfit_uniswapOfferNonZeroNonZero() {}
}
