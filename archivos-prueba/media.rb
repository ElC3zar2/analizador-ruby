# Complejidad media: metodo, array y acumulacion mediante for.
def sumar(a, b)
  return a + b
end

numeros = [1, 2, 3, 4]
total = 0
for numero in numeros
  total += numero
end

resultado = sumar(total, 10)
if resultado > 10
  puts "Resultado alto"
else
  puts "Resultado bajo"
end
puts resultado
