import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation, useQuery } from '@tanstack/react-query'
import { CreditCard, CheckCircle2, ChevronRight, Smartphone } from 'lucide-react'
import { ordersApi } from '@/api/orders'
import { paymentsApi } from '@/api/payments'
import { useCart } from '@/hooks/useCart'
import { formatPrice } from '@/utils'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import toast from 'react-hot-toast'

const addressSchema = z.object({
  recipientName: z.string().min(2, 'Name required'),
  phone:         z.string().min(10, 'Valid phone required'),
  postalCode:    z.string().min(7, 'Valid postal code required'),
  prefecture:    z.string().min(1, 'Prefecture required'),
  city:          z.string().min(1, 'City required'),
  addressLine1:  z.string().min(5, 'Address required'),
  addressLine2:  z.string().optional(),
  country:       z.string().default('JP'),
  buyerNote:     z.string().optional(),
})

type AddressForm = z.infer<typeof addressSchema>

type Step = 'shipping' | 'payment' | 'confirm' | 'success'

export function CheckoutPage() {
  const [step, setStep] = useState<Step>('shipping')
  const [orderId, setOrderId] = useState<string | null>(null)
  const [orderNumber, setOrderNumber] = useState<string | null>(null)
  const { cart } = useCart()
  const navigate = useNavigate()

  const { register, handleSubmit, getValues, formState: { errors } } = useForm<AddressForm>({
    resolver: zodResolver(addressSchema),
  })

  const FREE_SHIPPING = 3000
  const shippingFee = (cart?.subtotal ?? 0) >= FREE_SHIPPING ? 0 : 500
  const total = (cart?.subtotal ?? 0) + shippingFee

  const placeOrder = useMutation({
    mutationFn: (data: AddressForm) => ordersApi.place({
      shippingAddress: {
        recipientName: data.recipientName,
        phone: data.phone,
        addressLine1: data.addressLine1,
        addressLine2: data.addressLine2,
        city: data.city,
        prefecture: data.prefecture,
        postalCode: data.postalCode,
        country: data.country,
      },
      buyerNote: data.buyerNote,
    }),
    onSuccess: (res) => {
      setOrderId(res.data.data.id)
      setOrderNumber(res.data.data.orderNumber)
      setStep('payment')
    },
    onError: () => toast.error('Failed to place order'),
  })

  const initiatePayment = useMutation({
    mutationFn: () => paymentsApi.initiate(orderId!),
    onSuccess: (res) => {
      window.location.href = res.data.data.paymentUrl
    },
    onError: () => toast.error('Payment initiation failed'),
  })

  if (!cart || cart.items.length === 0) {
    return (
      <div className="max-w-7xl mx-auto px-4 py-20 text-center">
        <p className="font-display text-2xl mb-4">Your cart is empty</p>
        <Link to="/products" className="text-ember hover:underline">Browse Products</Link>
      </div>
    )
  }

  return (
    <div className="max-w-6xl mx-auto px-4 sm:px-6 py-8">
      <h1 className="page-title mb-8">Checkout</h1>

      {/* Steps indicator */}
      <div className="flex items-center gap-0 mb-10 overflow-x-auto">
        {(['shipping', 'payment', 'confirm'] as Step[]).map((s, i) => (
          <div key={s} className="flex items-center">
            <div className={`flex items-center gap-2.5 px-4 py-2 text-sm font-display font-semibold transition-all
              ${step === s ? 'bg-ink text-ash' : ['success'].includes(step) || (['payment','confirm'].includes(step) && s === 'shipping') || (step === 'confirm' && s === 'payment') ? 'text-sage' : 'text-ink/30'}`}
            >
              <span className="w-5 h-5 rounded-full border-2 flex items-center justify-center text-xs font-mono">
                {i + 1}
              </span>
              <span className="capitalize hidden sm:block">{s}</span>
            </div>
            {i < 2 && <ChevronRight size={14} className="text-ink/20 flex-shrink-0" />}
          </div>
        ))}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* ── Main Panel ── */}
        <div className="lg:col-span-2">

          {/* Step 1: Shipping */}
          {step === 'shipping' && (
            <div className="bg-white border border-ash-dark p-6">
              <h2 className="font-display font-bold text-xl mb-6">Shipping Information</h2>
              <form onSubmit={handleSubmit((data) => placeOrder.mutate(data))} className="space-y-4">
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <Input label="Recipient Name" error={errors.recipientName?.message} {...register('recipientName')} />
                  <Input label="Phone Number" error={errors.phone?.message} {...register('phone')} />
                </div>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <Input label="Postal Code" placeholder="123-4567" error={errors.postalCode?.message} {...register('postalCode')} />
                  <Input label="Prefecture" placeholder="Tokyo" error={errors.prefecture?.message} {...register('prefecture')} />
                </div>
                <Input label="City" error={errors.city?.message} {...register('city')} />
                <Input label="Address Line 1" placeholder="1-2-3 Shibuya" error={errors.addressLine1?.message} {...register('addressLine1')} />
                <Input label="Address Line 2 (optional)" placeholder="Apt, suite, etc." {...register('addressLine2')} />
                <div>
                  <label className="text-sm font-medium font-body text-ink/70 mb-1.5 block">Order Note (optional)</label>
                  <textarea
                    {...register('buyerNote')}
                    rows={3}
                    placeholder="Any special instructions…"
                    className="w-full border-2 border-ink/20 px-4 py-3 font-body text-sm text-ink placeholder:text-ink/35 focus:outline-none focus:border-ember transition-colors resize-none"
                  />
                </div>
                <Button type="submit" fullWidth size="lg" loading={placeOrder.isPending} className="mt-2">
                  Continue to Payment <ChevronRight size={16} />
                </Button>
              </form>
            </div>
          )}

          {/* Step 2: Payment */}
          {step === 'payment' && orderId && (
            <div className="bg-white border border-ash-dark p-6">
              <h2 className="font-display font-bold text-xl mb-6">Payment Method</h2>
              <div className="space-y-3">
                {/* PayPay */}
                <div className="border-2 border-ember p-5 bg-ember/3">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 bg-ember flex items-center justify-center">
                        <Smartphone size={18} className="text-white" />
                      </div>
                      <div>
                        <p className="font-display font-semibold text-sm">PayPay</p>
                        <p className="text-xs text-ink/50 font-body">Japan's #1 QR payment</p>
                      </div>
                    </div>
                    <div className="w-5 h-5 rounded-full border-2 border-ember bg-ember flex items-center justify-center">
                      <div className="w-2 h-2 rounded-full bg-white" />
                    </div>
                  </div>
                </div>

                {/* Credit Card (placeholder) */}
                <div className="border-2 border-ink/15 p-5 opacity-50 cursor-not-allowed">
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 bg-ink/8 flex items-center justify-center">
                      <CreditCard size={18} className="text-ink/40" />
                    </div>
                    <div>
                      <p className="font-display font-semibold text-sm text-ink/50">Credit / Debit Card</p>
                      <p className="text-xs text-ink/35 font-body">Coming soon</p>
                    </div>
                  </div>
                </div>
              </div>

              <div className="mt-6 space-y-3">
                <Button
                  fullWidth size="lg"
                  onClick={() => initiatePayment.mutate()}
                  loading={initiatePayment.isPending}
                >
                  Pay {formatPrice(total)} with PayPay
                </Button>
                <button
                  onClick={() => setStep('shipping')}
                  className="w-full text-sm font-body text-ink/40 hover:text-ink transition-colors"
                >
                  ← Back to Shipping
                </button>
              </div>
            </div>
          )}

          {/* Success */}
          {step === 'success' && (
            <div className="bg-white border border-ash-dark p-10 text-center">
              <div className="w-16 h-16 bg-sage/15 flex items-center justify-center mx-auto mb-5">
                <CheckCircle2 size={36} className="text-sage" />
              </div>
              <h2 className="font-display font-bold text-2xl mb-2">Order Placed!</h2>
              <p className="font-mono text-sm text-ink/50 mb-6">{orderNumber}</p>
              <p className="font-body text-sm text-ink/60 max-w-sm mx-auto mb-8">
                Thank you for your order. You'll receive a confirmation email shortly.
              </p>
              <div className="flex gap-3 justify-center">
                <Link to="/orders">
                  <Button variant="secondary">View Orders</Button>
                </Link>
                <Link to="/products">
                  <Button>Continue Shopping</Button>
                </Link>
              </div>
            </div>
          )}
        </div>

        {/* ── Order Summary ── */}
        <div>
          <div className="bg-white border border-ash-dark p-6 sticky top-24">
            <h2 className="font-display font-bold text-lg mb-5">Summary</h2>
            <div className="space-y-3 mb-4 max-h-64 overflow-y-auto">
              {cart.items.map(item => (
                <div key={item.id} className="flex gap-3">
                  <div className="w-12 h-12 bg-ash-dark flex-shrink-0 overflow-hidden relative">
                    {item.productImage && <img src={item.productImage} alt="" className="w-full h-full object-cover" />}
                    <span className="absolute -top-1 -right-1 w-5 h-5 bg-ink text-ash text-[10px] font-mono flex items-center justify-center rounded-full">
                      {item.quantity}
                    </span>
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-xs font-body text-ink line-clamp-1">{item.productName}</p>
                    <p className="text-sm font-display font-semibold mt-0.5">{formatPrice(item.lineTotal)}</p>
                  </div>
                </div>
              ))}
            </div>
            <div className="border-t border-ink/8 pt-4 space-y-2">
              <div className="flex justify-between text-sm font-body">
                <span className="text-ink/60">Subtotal</span>
                <span>{formatPrice(cart.subtotal)}</span>
              </div>
              <div className="flex justify-between text-sm font-body">
                <span className="text-ink/60">Shipping</span>
                <span className={shippingFee === 0 ? 'text-sage' : ''}>{shippingFee === 0 ? 'FREE' : formatPrice(shippingFee)}</span>
              </div>
              <div className="flex justify-between font-display font-bold pt-2 border-t border-ink/8">
                <span>Total</span>
                <span className="text-xl">{formatPrice(total)}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
