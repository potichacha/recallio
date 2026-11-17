def fizzbuzz(n):
    """Renvoie la liste FizzBuzz des nombres de 1 à n inclus."""
    resultat = []
    for i in range(1, n + 1):
        if i % 15 == 0:
            resultat.append("FizzBuzz")
        elif i % 3 == 0:
            resultat.append("Fizz")
        elif i % 5 == 0:
            resultat.append("Buzz")
        else:
            resultat.append(str(i))
    return resultat
