# Programa representativo del subconjunto sintactico.
def calcular(a, b)
  resultado = a + b * 2 ** 3
  if resultado >= 20 and not false
    return resultado
  elsif resultado == 0
    return 0
  else
    return -resultado
  end
end

@nombre = "Ruby"
@@contador = 0
$total = 3.14
datos = [1, 2, calcular(3, 4), :activo, true, nil]
edad = calcular(1, 2)
puts(edad)
while edad > 0 do
  edad -= 1
  if edad == 2
    next
  end
  if edad == 1
    break
  end
end
for elemento in 1...10 do
  puts elemento
end
x = 1; y = 2
