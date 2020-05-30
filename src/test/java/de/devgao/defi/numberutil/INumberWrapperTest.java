package devgao.io.numberutil;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static devgao.io.numberutil.NumberUtil.getMachineReadable;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class INumberWrapperTest {

    @Test
    public void divide() {
        Wad18 wad18 = new Wad18(BigInteger.ONE);
        Rad45 rad45 = new Rad45(BigInteger.ONE);
        Wad18 actual = wad18.divide(rad45);
        Wad18 expected = new Wad18(getMachineReadable(1.0));
        assertEquals(0, expected.compareTo(actual));
    }

    @Test
    public void multiply() {
    }

    @Test
    public void toBigInteger() {
    }

    @Test
    public void toBigDecimal() {
    }

    @Test
    public void testToString() {
    }
}
