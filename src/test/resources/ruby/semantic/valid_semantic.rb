saludar("Ruby")
def saludar(nombre)
  mensaje = "Hola " + nombre
  return mensaje
end
def sumar(a, b)
  return a + b
end
x = 10
x = "Ruby"
x += " lenguaje"
@valor = 1
@@contador = 0
$total = 0.5
datos = [1, "Ruby", true, nil, :activo, sumar(1, 2)]
limites = 1..10
for elemento in datos
  puts elemento
  next
end
puts elemento
for i in limites do
  $total += 1
  if i
    break
  end
end
contador = 3
while contador > 0 do
  contador -= 1
end
if x
  resultado = sumar(1, 2)
elsif @valor
  resultado = 0
else
  resultado = nil
end
puts resultado
