package devgao.io.maker;

import devgao.io.contractneedsprovider.*;
import devgao.io.gasprovider.GasProvider;
import devgao.io.util.JavaProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class MakerIT {
  private static final BigInteger minimumGasPrice = BigInteger.valueOf(1_000000000);
  private static final BigInteger maximumGasPrice = BigInteger.valueOf(200_000000000L);
  Maker maker;

  @BeforeEach
  public void setUp() {
    JavaProperties javaProperties = new JavaProperties(true);
    String ethereumAddress = javaProperties.getValue("myEthereumAddress");
    String password = javaProperties.getValue("password");
    String infuraProjectId = javaProperties.getValue("infuraProjectId");
    CircuitBreaker circuitBreaker = new CircuitBreaker();
    Web3j web3j = new Web3jProvider(infuraProjectId).web3j;
    Credentials credentials = new Wallet(password, ethereumAddress, true).getCredentials();
    GasProvider gasProvider = new GasProvider(web3j, minimumGasPrice, maximumGasPrice);
    Permissions permissions = new Permissions(true, true);
    ContractNeedsProvider contractNeedsProvider =
        new ContractNeedsProvider(web3j, credentials, gasProvider, permissions, circuitBreaker);
    maker = new Maker(contractNeedsProvider);
  }

  @Test
  public void isContractValid_isValid_continueRunning() {
    assertDoesNotThrow(() -> maker.isContractValid());
  }

  @Test
  public void isContractValid_isNotValid_stopRunning() {}
}
