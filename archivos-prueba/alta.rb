# Complejidad alta: ciclos y condicionales anidados dentro de un metodo.
def calcular(limite, incremento)
  total = 0
  for i in 1..limite do
    if i % 2 == 0 and i >= 2
      total += i * incremento
    elsif i == 1
      total += 1
    else
      total += i - 1
    end
  end

  contador = 0
  while contador < 3 do
    if contador != 2
      total += contador ** 2
    else
      total %= 100
    end
    contador += 1
  end
  return total
end

datos = [1, 2, 3]
resultado = calcular(10, 2)
acumulado = 0
for dato in datos
  acumulado += calcular(dato, 1)
end
if resultado > 20 || acumulado > 10
  puts "Resultado alto"
elsif resultado == 20
  puts "Resultado exacto"
else
  puts "Resultado bajo"
end
puts resultado
puts acumulado
