import React, { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { CreditCard, CheckCircle2, XCircle, Clock, ExternalLink, RefreshCw } from 'lucide-react'
import { paymentApi, orderApi } from '@/api/services'
import { Button, Spinner } from '@/components/ui'
import { formatPrice } from '@/utils'
import toast from 'react-hot-toast'

export const PaymentPage = () => {
  const { orderId } = useParams<{ orderId: string }>()
  const navigate = useNavigate()
  const [initiating, setInitiating] = useState(false)
  const [paymentInfo, setPaymentInfo] = useState<any>(null)

  const { data: order } = useQuery({
    queryKey: ['order', orderId],
    queryFn:  () => orderApi.detail(orderId!),
    enabled:  !!orderId,
  })

  // Poll payment status every 5s while pending
  const { data: paymentStatus, refetch } = useQuery({
    queryKey: ['payment-status', orderId],
    queryFn:  () => paymentApi.status(orderId!),
    enabled:  !!orderId && !!paymentInfo,
    refetchInterval: (data: any) => {
      if (!data || data?.status === 'INITIATED' || data?.status === 'AWAITING_CAPTURE') return 5000
      return false
    },
  })

  const handleInitiatePayment = async () => {
    if (!orderId) return
    setInitiating(true)
    try {
      const payment = await paymentApi.initiate(orderId)
      setPaymentInfo(payment)
      // On mobile, try deeplink first
      if (payment.deeplink) {
        window.location.href = payment.deeplink
        // Fallback to web URL after 2s if app not installed
        setTimeout(() => { if (payment.paymentUrl) window.open(payment.paymentUrl, '_blank') }, 2000)
      } else if (payment.paymentUrl) {
        window.open(payment.paymentUrl, '_blank')
      }
    } catch {
      toast.error('Could not initiate payment. Please try again.')
    } finally {
      setInitiating(false)
    }
  }

  const status = paymentStatus?.status || 'IDLE'
  const isComplete = status === 'COMPLETED'
  const isFailed   = status === 'FAILED' || status === 'CANCELLED' || status === 'EXPIRED'

  useEffect(() => {
    if (isComplete) {
      toast.success('Payment confirmed!')
      setTimeout(() => navigate(`/orders/${orderId}`), 1500)
    }
  }, [isComplete])

  if (!order) return (
    <div className="min-h-[60vh] flex items-center justify-center"><Spinner size="lg" /></div>
  )

  return (
    <div className="container-page py-12 max-w-lg mx-auto">
      <div className="card-lg p-8 text-center animate-fade-up">

        {/* Status icon */}
        <div className="mb-6">
          {isComplete ? (
            <div className="w-20 h-20 rounded-full bg-green-50 flex items-center justify-center mx-auto">
              <CheckCircle2 className="h-10 w-10 text-success" />
            </div>
          ) : isFailed ? (
            <div className="w-20 h-20 rounded-full bg-red-50 flex items-center justify-center mx-auto">
              <XCircle className="h-10 w-10 text-error" />
            </div>
          ) : paymentInfo ? (
            <div className="w-20 h-20 rounded-full bg-amber-50 flex items-center justify-center mx-auto">
              <Clock className="h-10 w-10 text-warning animate-pulse" />
            </div>
          ) : (
            <div className="w-20 h-20 rounded-full bg-accent-light flex items-center justify-center mx-auto">
              <CreditCard className="h-10 w-10 text-accent" />
            </div>
          )}
        </div>

        {/* Title */}
        <h1 className="font-display text-2xl font-semibold mb-2">
          {isComplete ? 'Payment Confirmed!' :
           isFailed   ? 'Payment Failed' :
           paymentInfo ? 'Awaiting Payment' :
           'Complete Your Payment'}
        </h1>

        <p className="text-sm text-ink-muted mb-1">Order: <span className="font-mono font-medium">{order.orderNumber}</span></p>
        <p className="font-display text-3xl font-semibold mt-3 mb-6">{formatPrice(order.totalAmount)}</p>

        {/* Content */}
        {isComplete && (
          <div className="space-y-3">
            <p className="text-sm text-ink-muted">Redirecting to your order…</p>
            <Button variant="accent" onClick={() => navigate(`/orders/${orderId}`)}>View Order</Button>
          </div>
        )}

        {isFailed && (
          <div className="space-y-3">
            <p className="text-sm text-error">Your payment could not be processed.</p>
            <div className="flex gap-3 justify-center">
              <Button variant="outline" onClick={() => navigate('/orders')}>View Orders</Button>
              <Button variant="accent" onClick={handleInitiatePayment} loading={initiating}>Try Again</Button>
            </div>
          </div>
        )}

        {!paymentInfo && !isComplete && !isFailed && (
          <div className="space-y-4">
            <p className="text-sm text-ink-muted leading-relaxed">
              You'll be redirected to PayPay to complete your payment securely.
            </p>
            <Button variant="accent" size="lg" className="w-full"
              onClick={handleInitiatePayment} loading={initiating}
              icon={<CreditCard className="h-5 w-5" />}>
              Pay with PayPay
            </Button>
            <Button variant="ghost" size="sm" onClick={() => navigate(`/orders/${orderId}`)}>
              Pay later
            </Button>
          </div>
        )}

        {paymentInfo && !isComplete && !isFailed && (
          <div className="space-y-4">
            <p className="text-sm text-ink-muted">
              Complete your payment in the PayPay app or browser window.
            </p>
            {paymentInfo.paymentUrl && (
              <a href={paymentInfo.paymentUrl} target="_blank" rel="noopener noreferrer">
                <Button variant="outline" size="sm" icon={<ExternalLink className="h-4 w-4" />}>
                  Open PayPay
                </Button>
              </a>
            )}
            <div className="flex items-center justify-center gap-2 text-xs text-ink-muted">
              <Spinner size="sm" />
              Waiting for payment confirmation…
            </div>
            <Button variant="ghost" size="sm" onClick={() => refetch()} icon={<RefreshCw className="h-4 w-4" />}>
              Check status
            </Button>
          </div>
        )}

        {paymentInfo?.expiresAt && !isComplete && !isFailed && (
          <p className="text-xs text-ink-muted mt-4">
            Payment link expires at {new Date(paymentInfo.expiresAt).toLocaleTimeString()}
          </p>
        )}
      </div>
    </div>
  )
}
