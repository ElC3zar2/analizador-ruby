# Subconjunto academico: fixture lexico, sin validar sintaxis.
def saludar(nombre_usuario)
  @nombre = "Hola \"Ruby\" #{nombre_usuario}"
  @@contador += 1
  $total = 25.75
  estado = :activo
  puts 'Ruby\n\t\\'
  if true and not false || nil
    edad = -25 + 3 * 2 ** 4 / 2 % 3
  elsif edad >= 20 && edad <= 100
    edad -= 1; edad *= 2; edad /= 2; edad %= 5
  else
    return [0, 25, 1234, 0.5, {estado: :nombre}]
  end
  while edad != 0 do
    break
    next
  end
  for dato2 in 1..10 do
    puts dato2
  end
  for _dato in 1...10 do
    puts _dato
  end
  a = edad == 20 or edad > 10
  b = !a; c = edad < 100
  objeto.metodo(:estado)
end
