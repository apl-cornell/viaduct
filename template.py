next_base = MemValue(regint(0))

def array_alloc(size):
  global next_base
  arr_base = next_base.read()
  next_base.write(next_base.read() + size)
  return arr_base


def reg_write(memreg, val):
  if isinstance(val, sbit):
    val = sregint(1) & val

  memreg.write(val)


def reg_read(memreg):
  return memreg.read()


def secret_load(arr, i):
  val = sregint.load_mem(arr + i)
  return val


def clear_load(arr, i):
  val = regint.load_mem(arr + i)
  return val


def secret_store(arr, i, val):
  if isinstance(val, sbit):
    val = sregint(1) & val

  val.store_in_mem(arr + i)


def clear_store(arr, i, v):
  v.store_in_mem(arr + i)


def get_input(n):
  input = sint.get_private_input_from(n)
  input_reg = sregint()
  input_reg.load_secret(input)
  return input_reg


def send_secret_output(sreg, player_num):
  if isinstance(sreg, sbit):
    sreg = sregint(1) & sreg

  sval = sint()
  sval.load_secret(sreg)
  sval.reveal_to(player_num)


def send_clear_output(reg, player_num):
  val = cint(reg)
  val.public_output(player_num)


obj = {}
