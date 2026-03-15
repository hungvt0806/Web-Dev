import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { cartApi } from '@/api/cart'
import { useCartStore } from '@/store/cartStore'
import toast from 'react-hot-toast'

export function useCart() {
  const { setCart } = useCartStore()
  const qc = useQueryClient()

  const { data, isLoading } = useQuery({
    queryKey: ['cart'],
    queryFn: async () => {
      const res = await cartApi.getCart()
      setCart(res.data.data)
      return res.data.data
    },
    staleTime: 30_000,
  })

  const addItem = useMutation({
    mutationFn: cartApi.addItem,
    onSuccess: (res) => {
      setCart(res.data.data)
      qc.invalidateQueries({ queryKey: ['cart'] })
      toast.success('Added to cart')
    },
  })

  const updateItem = useMutation({
    mutationFn: ({ cartItemId, quantity }: { cartItemId: number; quantity: number }) =>
      cartApi.updateItem(cartItemId, quantity),
    onSuccess: (res) => {
      setCart(res.data.data)
      qc.invalidateQueries({ queryKey: ['cart'] })
    },
  })

  const removeItem = useMutation({
    mutationFn: cartApi.removeItem,
    onSuccess: (res) => {
      setCart(res.data.data)
      qc.invalidateQueries({ queryKey: ['cart'] })
      toast.success('Removed from cart')
    },
  })

  const clearCart = useMutation({
    mutationFn: cartApi.clearCart,
    onSuccess: () => {
      setCart({ id: 0, items: [], itemCount: 0, totalQuantity: 0, subtotal: 0, hasStalePrices: false })
      qc.invalidateQueries({ queryKey: ['cart'] })
    },
  })

  return { cart: data, isLoading, addItem, updateItem, removeItem, clearCart }
}
