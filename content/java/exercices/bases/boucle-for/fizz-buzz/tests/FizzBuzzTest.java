import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FizzBuzzTest {

    @Test
    void remplaceLesMultiplesDeTroisEtDeCinq() {
        assertEquals("1 2 Fizz 4 Buzz", FizzBuzz.jusqua(5));
    }

    @Test
    void remplaceLesMultiplesDeQuinzeParFizzBuzz() {
        assertEquals("1 2 Fizz 4 Buzz Fizz 7 8 Fizz Buzz 11 Fizz 13 14 FizzBuzz", FizzBuzz.jusqua(15));
    }

    @Test
    void neCommenceNiNeFinitParUneEspace() {
        assertEquals("1", FizzBuzz.jusqua(1));
    }

    @Test
    void renvoieUneChaineVidePourZero() {
        assertEquals("", FizzBuzz.jusqua(0));
    }
}
